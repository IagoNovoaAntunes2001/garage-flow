data "archive_file" "authorizer" {
  count       = var.enable_gateway ? 1 : 0
  type        = "zip"
  source_file = "${path.module}/authorizer/handler.py"
  output_path = "${path.module}/.terraform/authorizer.zip"
}

data "aws_iam_policy_document" "lambda_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "authorizer" {
  count              = var.enable_gateway ? 1 : 0
  name               = "${var.cluster_name}-authorizer"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume.json
  tags               = local.tags
}

resource "aws_iam_role_policy_attachment" "authorizer_logs" {
  count      = var.enable_gateway ? 1 : 0
  role       = aws_iam_role.authorizer[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "authorizer_secret" {
  count = var.enable_gateway ? 1 : 0
  role  = aws_iam_role.authorizer[0].id
  policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = var.jwt_secret_arn }]
  })
}

resource "aws_lambda_function" "authorizer" {
  count            = var.enable_gateway ? 1 : 0
  function_name    = "${var.cluster_name}-authorizer"
  role             = aws_iam_role.authorizer[0].arn
  handler          = "handler.handler"
  runtime          = "python3.12"
  filename         = data.archive_file.authorizer[0].output_path
  source_code_hash = data.archive_file.authorizer[0].output_base64sha256
  timeout          = 10
  environment {
    variables = { JWT_SECRET_ARN = var.jwt_secret_arn, JWT_ISSUER = var.jwt_issuer }
  }
  tags = local.tags
}

resource "aws_security_group" "vpc_link" {
  count       = var.enable_gateway ? 1 : 0
  name        = "${var.cluster_name}-api-link"
  description = "API Gateway VPC link to internal NLB"
  vpc_id      = module.vpc.vpc_id
  egress {
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.nlb[0].id]
  }
  tags = local.tags
}

resource "aws_security_group" "nlb" {
  count       = var.enable_gateway ? 1 : 0
  name        = "${var.cluster_name}-internal-nlb"
  description = "Internal NLB ingress from API Gateway"
  vpc_id      = module.vpc.vpc_id
  tags        = local.tags
}

resource "aws_vpc_security_group_ingress_rule" "nlb_from_link" {
  count                        = var.enable_gateway ? 1 : 0
  security_group_id            = aws_security_group.nlb[0].id
  referenced_security_group_id = aws_security_group.vpc_link[0].id
  ip_protocol                  = "tcp"
  from_port                    = 80
  to_port                      = 80
}

resource "aws_vpc_security_group_egress_rule" "nlb_to_nodes" {
  count                        = var.enable_gateway ? 1 : 0
  security_group_id            = aws_security_group.nlb[0].id
  referenced_security_group_id = module.eks.node_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = 30080
  to_port                      = 30080
}

resource "aws_vpc_security_group_ingress_rule" "nodes_from_nlb" {
  count                        = var.enable_gateway ? 1 : 0
  security_group_id            = module.eks.node_security_group_id
  referenced_security_group_id = aws_security_group.nlb[0].id
  ip_protocol                  = "tcp"
  from_port                    = 30080
  to_port                      = 30080
}

resource "aws_lb" "gateway" {
  count              = var.enable_gateway ? 1 : 0
  name               = "${substr(var.cluster_name, 0, 22)}-api"
  internal           = true
  load_balancer_type = "network"
  subnets            = module.vpc.private_subnets
  security_groups    = [aws_security_group.nlb[0].id]
  tags               = local.tags
}

resource "aws_lb_target_group" "application" {
  count       = var.enable_gateway ? 1 : 0
  name        = "${substr(var.cluster_name, 0, 22)}-api"
  port        = 30080
  protocol    = "TCP"
  target_type = "instance"
  vpc_id      = module.vpc.vpc_id
  health_check {
    protocol = "HTTP"
    path     = "/actuator/health/readiness"
    port     = "traffic-port"
    matcher  = "200"
  }
  tags = local.tags
}

resource "aws_autoscaling_attachment" "application" {
  count                  = var.enable_gateway ? 1 : 0
  autoscaling_group_name = module.eks.eks_managed_node_groups["application"].node_group_autoscaling_group_names[0]
  lb_target_group_arn    = aws_lb_target_group.application[0].arn
}

resource "aws_lb_listener" "application" {
  count             = var.enable_gateway ? 1 : 0
  load_balancer_arn = aws_lb.gateway[0].arn
  port              = 80
  protocol          = "TCP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.application[0].arn
  }
}

resource "aws_apigatewayv2_api" "application" {
  count         = var.enable_gateway ? 1 : 0
  name          = "${var.cluster_name}-api"
  protocol_type = "HTTP"
  tags          = local.tags
}

resource "aws_apigatewayv2_vpc_link" "application" {
  count              = var.enable_gateway ? 1 : 0
  name               = "${var.cluster_name}-link"
  subnet_ids         = module.vpc.private_subnets
  security_group_ids = [aws_security_group.vpc_link[0].id]
  tags               = local.tags
}

resource "aws_apigatewayv2_integration" "application" {
  count                  = var.enable_gateway ? 1 : 0
  api_id                 = aws_apigatewayv2_api.application[0].id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = aws_lb_listener.application[0].arn
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.application[0].id
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_authorizer" "application" {
  count                             = var.enable_gateway ? 1 : 0
  api_id                            = aws_apigatewayv2_api.application[0].id
  authorizer_type                   = "REQUEST"
  name                              = "administrator-jwt"
  authorizer_uri                    = aws_lambda_function.authorizer[0].invoke_arn
  authorizer_payload_format_version = "2.0"
  enable_simple_responses           = true
  authorizer_result_ttl_in_seconds  = 0
  identity_sources                  = ["$request.header.Authorization"]
}

resource "aws_lambda_permission" "api_gateway" {
  count         = var.enable_gateway ? 1 : 0
  statement_id  = "AllowApiGatewayAuthorizer"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.authorizer[0].function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.application[0].execution_arn}/authorizers/${aws_apigatewayv2_authorizer.application[0].id}"
}

resource "aws_apigatewayv2_route" "admin" {
  for_each           = var.enable_gateway ? toset(["ANY /api/v1/admin", "ANY /api/v1/admin/{proxy+}"]) : toset([])
  api_id             = aws_apigatewayv2_api.application[0].id
  route_key          = each.value
  target             = "integrations/${aws_apigatewayv2_integration.application[0].id}"
  authorization_type = "CUSTOM"
  authorizer_id      = aws_apigatewayv2_authorizer.application[0].id
}

resource "aws_apigatewayv2_route" "public" {
  count     = var.enable_gateway ? 1 : 0
  api_id    = aws_apigatewayv2_api.application[0].id
  route_key = "$default"
  target    = "integrations/${aws_apigatewayv2_integration.application[0].id}"
}

resource "aws_apigatewayv2_stage" "application" {
  count       = var.enable_gateway ? 1 : 0
  api_id      = aws_apigatewayv2_api.application[0].id
  name        = "$default"
  auto_deploy = true
  tags        = local.tags
}
