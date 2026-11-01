output "cluster_name" {
  value = module.eks.cluster_name
}

output "database_endpoint" {
  value = aws_db_instance.database.address
}

output "database_master_secret_arn" {
  value = aws_db_instance.database.master_user_secret[0].secret_arn
}

output "image_repository_url" {
  value = aws_ecr_repository.application.repository_url
}

output "api_gateway_url" {
  value = var.enable_gateway ? aws_apigatewayv2_stage.application[0].invoke_url : null
}

output "application_secrets_role_arn" {
  value = aws_iam_role.application_secrets.arn
}
