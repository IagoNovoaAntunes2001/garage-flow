resource "aws_iam_role" "codebuild_database" {
  count              = var.enable_database_runner ? 1 : 0
  name               = "${var.cluster_name}-codebuild-database"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume.json
  tags               = local.tags
}

resource "aws_iam_role_policy_attachment" "codebuild_database_readonly" {
  count      = var.enable_database_runner ? 1 : 0
  role       = aws_iam_role.codebuild_database[0].name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

resource "aws_cloudwatch_log_group" "codebuild_database" {
  count             = var.enable_database_runner ? 1 : 0
  name              = "/aws/codebuild/${var.cluster_name}-database-runner"
  retention_in_days = 7
  tags              = local.tags
}

data "aws_iam_policy_document" "codebuild_database" {
  count = var.enable_database_runner ? 1 : 0

  statement {
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.codebuild_database[0].arn}:*"]
  }
  statement {
    actions   = ["codeconnections:GetConnection", "codeconnections:GetConnectionToken", "codeconnections:UseConnection", "codestar-connections:GetConnection", "codestar-connections:GetConnectionToken", "codestar-connections:UseConnection"]
    resources = [var.github_connection_arn]
  }
  statement {
    actions   = ["rds:CreateDBInstance", "rds:ModifyDBInstance", "rds:AddTagsToResource", "rds:RemoveTagsFromResource"]
    resources = ["arn:aws:rds:${var.aws_region}:${data.aws_caller_identity.current.account_id}:db:${var.cluster_name}-postgres", "arn:aws:rds:${var.aws_region}:${data.aws_caller_identity.current.account_id}:subgrp:${var.cluster_name}-database"]
  }
  statement {
    actions   = ["rds:CreateDBSubnetGroup", "rds:ModifyDBSubnetGroup", "rds:AddTagsToResource", "rds:RemoveTagsFromResource"]
    resources = ["arn:aws:rds:${var.aws_region}:${data.aws_caller_identity.current.account_id}:subgrp:${var.cluster_name}-database"]
  }
  statement {
    actions   = ["secretsmanager:CreateSecret", "secretsmanager:TagResource"]
    resources = ["arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:rds!db-*"]
  }
  statement {
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = ["arn:aws:s3:::${var.terraform_state_bucket}"]
  }
  statement {
    actions   = ["s3:GetObject", "s3:PutObject"]
    resources = ["arn:aws:s3:::${var.terraform_state_bucket}/garage-flow/terraform.tfstate"]
  }
  statement {
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["arn:aws:s3:::${var.terraform_state_bucket}/garage-flow/terraform.tfstate.tflock"]
  }
}

resource "aws_iam_role_policy" "codebuild_database" {
  count  = var.enable_database_runner ? 1 : 0
  role   = aws_iam_role.codebuild_database[0].id
  policy = data.aws_iam_policy_document.codebuild_database[0].json
}

resource "aws_codebuild_project" "database_runner" {
  count         = var.enable_database_runner ? 1 : 0
  name          = "${var.cluster_name}-database-runner"
  service_role  = aws_iam_role.codebuild_database[0].arn
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
    cloudwatch_logs { group_name = aws_cloudwatch_log_group.codebuild_database[0].name }
  }
  tags       = local.tags
  depends_on = [aws_iam_role_policy.codebuild_database, aws_iam_role_policy_attachment.codebuild_database_readonly]
}

resource "aws_codebuild_webhook" "database_runner" {
  count        = var.enable_database_runner ? 1 : 0
  project_name = aws_codebuild_project.database_runner[0].name
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
