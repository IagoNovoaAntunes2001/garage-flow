data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "codebuild_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codebuild.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_iam_role" "codebuild_deploy" {
  count              = var.enable_codebuild_runner ? 1 : 0
  name               = "${var.cluster_name}-codebuild-deploy"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume.json
  tags               = local.tags
}

resource "aws_cloudwatch_log_group" "codebuild_deploy" {
  count             = var.enable_codebuild_runner ? 1 : 0
  name              = "/aws/codebuild/${var.cluster_name}-deploy"
  retention_in_days = 7
  tags              = local.tags
}

data "aws_iam_policy_document" "codebuild_deploy" {
  count = var.enable_codebuild_runner ? 1 : 0

  statement {
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.codebuild_deploy[0].arn}:*"]
  }
  statement {
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }
  statement {
    actions   = ["ecr:BatchCheckLayerAvailability", "ecr:CompleteLayerUpload", "ecr:InitiateLayerUpload", "ecr:PutImage", "ecr:UploadLayerPart"]
    resources = [aws_ecr_repository.application.arn]
  }
  statement {
    actions   = ["eks:DescribeCluster"]
    resources = [module.eks.cluster_arn]
  }
  statement {
    actions   = ["rds:DescribeDBInstances"]
    resources = ["*"]
  }
  statement {
    actions   = ["codeconnections:GetConnection", "codeconnections:GetConnectionToken", "codeconnections:UseConnection"]
    resources = [var.github_connection_arn]
  }
  statement {
    actions   = ["ec2:CreateNetworkInterface", "ec2:DeleteNetworkInterface", "ec2:DescribeDhcpOptions", "ec2:DescribeNetworkInterfaces", "ec2:DescribeSecurityGroups", "ec2:DescribeSubnets", "ec2:DescribeVpcs"]
    resources = ["*"]
  }
  statement {
    actions   = ["ec2:CreateNetworkInterfacePermission"]
    resources = ["arn:aws:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:network-interface/*"]
    condition {
      test     = "StringEquals"
      variable = "ec2:AuthorizedService"
      values   = ["codebuild.amazonaws.com"]
    }
    condition {
      test     = "ArnEquals"
      variable = "ec2:Subnet"
      values   = [for subnet in module.vpc.private_subnets : "arn:aws:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:subnet/${subnet}"]
    }
  }
}

resource "aws_iam_role_policy" "codebuild_deploy" {
  count  = var.enable_codebuild_runner ? 1 : 0
  role   = aws_iam_role.codebuild_deploy[0].id
  policy = data.aws_iam_policy_document.codebuild_deploy[0].json
}

resource "aws_security_group" "codebuild_deploy" {
  count       = var.enable_codebuild_runner ? 1 : 0
  name        = "${var.cluster_name}-codebuild"
  description = "Outbound access for the ephemeral deployment runner"
  vpc_id      = module.vpc.vpc_id
  egress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = local.tags
}

resource "aws_vpc_security_group_ingress_rule" "eks_from_codebuild" {
  count                        = var.enable_codebuild_runner ? 1 : 0
  security_group_id            = module.eks.cluster_security_group_id
  referenced_security_group_id = aws_security_group.codebuild_deploy[0].id
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
}

resource "aws_codebuild_project" "deploy_runner" {
  count         = var.enable_codebuild_runner ? 1 : 0
  name          = "${var.cluster_name}-deploy"
  service_role  = aws_iam_role.codebuild_deploy[0].arn
  build_timeout = 60

  artifacts { type = "NO_ARTIFACTS" }
  environment {
    compute_type                = "BUILD_GENERAL1_MEDIUM"
    image                       = "aws/codebuild/standard:7.0"
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode             = true
  }
  source {
    type     = "GITHUB"
    location = "https://github.com/IagoNovoaAntunes2001/garage-flow.git"
    auth {
      type     = "CODECONNECTIONS"
      resource = var.github_connection_arn
    }
  }
  vpc_config {
    vpc_id             = module.vpc.vpc_id
    subnets            = module.vpc.private_subnets
    security_group_ids = [aws_security_group.codebuild_deploy[0].id]
  }
  logs_config {
    cloudwatch_logs { group_name = aws_cloudwatch_log_group.codebuild_deploy[0].name }
  }
  tags       = local.tags
  depends_on = [aws_iam_role_policy.codebuild_deploy]
}

resource "aws_codebuild_webhook" "deploy_runner" {
  count        = var.enable_codebuild_runner ? 1 : 0
  project_name = aws_codebuild_project.deploy_runner[0].name
  build_type   = "BUILD"
  filter_group {
    filter {
      type    = "EVENT"
      pattern = "WORKFLOW_JOB_QUEUED"
    }
    filter {
      type    = "WORKFLOW_NAME"
      pattern = "^Build and test$"
    }
    filter {
      type    = "ACTOR_ACCOUNT_ID"
      pattern = "^${var.github_deploy_actor_id}$"
    }
  }
}
