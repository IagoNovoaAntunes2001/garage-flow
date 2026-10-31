resource "aws_iam_role" "codebuild_karpenter" {
  count              = var.enable_karpenter ? 1 : 0
  name               = "${var.cluster_name}-codebuild-karpenter"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume.json
  tags               = local.tags
}

resource "aws_cloudwatch_log_group" "codebuild_karpenter" {
  count             = var.enable_karpenter ? 1 : 0
  name              = "/aws/codebuild/${var.cluster_name}-karpenter"
  retention_in_days = 7
  tags              = local.tags
}

data "aws_iam_policy_document" "codebuild_karpenter" {
  count = var.enable_karpenter ? 1 : 0

  statement {
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.codebuild_karpenter[0].arn}:*"]
  }
  statement {
    actions   = ["eks:DescribeCluster"]
    resources = [module.eks.cluster_arn]
  }
  statement {
    actions   = ["codeconnections:GetConnection", "codeconnections:GetConnectionToken", "codeconnections:UseConnection", "codestar-connections:GetConnection", "codestar-connections:GetConnectionToken", "codestar-connections:UseConnection"]
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

resource "aws_iam_role_policy" "codebuild_karpenter" {
  count  = var.enable_karpenter ? 1 : 0
  role   = aws_iam_role.codebuild_karpenter[0].id
  policy = data.aws_iam_policy_document.codebuild_karpenter[0].json
}

resource "aws_codebuild_project" "karpenter_runner" {
  count         = var.enable_karpenter ? 1 : 0
  name          = "${var.cluster_name}-karpenter"
  service_role  = aws_iam_role.codebuild_karpenter[0].arn
  build_timeout = 30

  artifacts { type = "NO_ARTIFACTS" }
  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/standard:7.0"
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "CODEBUILD"
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
    cloudwatch_logs { group_name = aws_cloudwatch_log_group.codebuild_karpenter[0].name }
  }
  tags       = local.tags
  depends_on = [aws_iam_role_policy.codebuild_karpenter]
}

resource "aws_codebuild_webhook" "karpenter_runner" {
  count        = var.enable_karpenter ? 1 : 0
  project_name = aws_codebuild_project.karpenter_runner[0].name
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
