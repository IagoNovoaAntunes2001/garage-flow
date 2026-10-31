resource "aws_iam_role" "codebuild_plan" {
  count              = var.enable_codebuild_runner ? 1 : 0
  name               = "${var.cluster_name}-codebuild-plan"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume.json
  tags               = local.tags
}

resource "aws_iam_role_policy_attachment" "codebuild_plan_readonly" {
  count      = var.enable_codebuild_runner ? 1 : 0
  role       = aws_iam_role.codebuild_plan[0].name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

resource "aws_cloudwatch_log_group" "codebuild_plan" {
  count             = var.enable_codebuild_runner ? 1 : 0
  name              = "/aws/codebuild/${var.cluster_name}-plan"
  retention_in_days = 7
  tags              = local.tags
}

data "aws_iam_policy_document" "codebuild_plan" {
  count = var.enable_codebuild_runner ? 1 : 0

  statement {
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.codebuild_plan[0].arn}:*"]
  }
  statement {
    actions   = ["codeconnections:GetConnection", "codeconnections:GetConnectionToken", "codeconnections:UseConnection", "codestar-connections:GetConnection", "codestar-connections:GetConnectionToken", "codestar-connections:UseConnection"]
    resources = [var.github_connection_arn]
  }
  statement {
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = ["arn:aws:s3:::${var.terraform_state_bucket}"]
  }
  statement {
    actions   = ["s3:GetObject"]
    resources = ["arn:aws:s3:::${var.terraform_state_bucket}/garage-flow/terraform.tfstate"]
  }
  statement {
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["arn:aws:s3:::${var.terraform_state_bucket}/garage-flow/terraform.tfstate.tflock"]
  }
}

resource "aws_iam_role_policy" "codebuild_plan" {
  count  = var.enable_codebuild_runner ? 1 : 0
  role   = aws_iam_role.codebuild_plan[0].id
  policy = data.aws_iam_policy_document.codebuild_plan[0].json
}

resource "aws_codebuild_project" "plan_runner" {
  count         = var.enable_codebuild_runner ? 1 : 0
  name          = "${var.cluster_name}-plan"
  service_role  = aws_iam_role.codebuild_plan[0].arn
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
  logs_config {
    cloudwatch_logs { group_name = aws_cloudwatch_log_group.codebuild_plan[0].name }
  }
  tags       = local.tags
  depends_on = [aws_iam_role_policy.codebuild_plan, aws_iam_role_policy_attachment.codebuild_plan_readonly]
}

resource "aws_codebuild_webhook" "plan_runner" {
  count        = var.enable_codebuild_runner ? 1 : 0
  project_name = aws_codebuild_project.plan_runner[0].name
  build_type   = "BUILD"
  filter_group {
    filter {
      type    = "EVENT"
      pattern = "WORKFLOW_JOB_QUEUED"
    }
    filter {
      type    = "WORKFLOW_NAME"
      pattern = "^Plan AWS infrastructure$"
    }
    filter {
      type    = "ACTOR_ACCOUNT_ID"
      pattern = "^${var.github_deploy_actor_id}$"
    }
  }
}
