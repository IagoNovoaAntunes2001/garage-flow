data "aws_secretsmanager_secret" "application_database" {
  name = "${var.cluster_name}/database-app"
}

data "aws_secretsmanager_secret" "application_jwt" {
  name = "${var.cluster_name}/jwt"
}

data "aws_secretsmanager_secret" "application_bootstrap_admin" {
  name = "${var.cluster_name}/bootstrap-admin"
}

data "aws_secretsmanager_secret" "application_integration" {
  name = "${var.cluster_name}/integration"
}

data "aws_iam_policy_document" "application_secrets_assume" {
  statement {
    actions = ["sts:AssumeRole", "sts:TagSession"]
    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "application_secrets" {
  name               = "${var.cluster_name}-application-secrets"
  assume_role_policy = data.aws_iam_policy_document.application_secrets_assume.json
  tags               = local.tags
}

data "aws_iam_policy_document" "application_secrets" {
  statement {
    sid     = "ReadApplicationSecrets"
    actions = ["secretsmanager:GetSecretValue"]
    resources = [
      data.aws_secretsmanager_secret.application_database.arn,
      data.aws_secretsmanager_secret.application_jwt.arn,
      data.aws_secretsmanager_secret.application_bootstrap_admin.arn,
      data.aws_secretsmanager_secret.application_integration.arn,
    ]
  }
}

resource "aws_iam_role_policy" "application_secrets" {
  name   = "read-application-secrets"
  role   = aws_iam_role.application_secrets.id
  policy = data.aws_iam_policy_document.application_secrets.json
}

resource "aws_eks_pod_identity_association" "application_secrets" {
  cluster_name    = module.eks.cluster_name
  namespace       = "default"
  service_account = "garage-flow"
  role_arn        = aws_iam_role.application_secrets.arn
}
