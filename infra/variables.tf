variable "aws_region" {
  description = "AWS region selected for this environment"
  type        = string
}

variable "cluster_name" {
  description = "EKS cluster and resource name prefix"
  type        = string
  default     = "garage-flow"
}

variable "eks_public_access_cidrs" {
  description = "Trusted CIDRs allowed to reach the EKS API endpoint"
  type        = list(string)
  validation {
    condition     = length(var.eks_public_access_cidrs) > 0 && !contains(var.eks_public_access_cidrs, "0.0.0.0/0")
    error_message = "Provide at least one restricted CIDR; 0.0.0.0/0 is not permitted."
  }
}

variable "kubernetes_version" {
  description = "EKS-supported Kubernetes version selected for the target region"
  type        = string
}

variable "node_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "db_multi_az" {
  description = "Enable RDS standby in another availability zone; increases cost"
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  description = "Automated backup retention in days; verify the free-plan account limit"
  type        = number
  default     = 1
  validation {
    condition     = var.db_backup_retention_days >= 0 && var.db_backup_retention_days <= 35 && floor(var.db_backup_retention_days) == var.db_backup_retention_days
    error_message = "Backup retention must be a whole number between 0 and 35."
  }
}

variable "enable_gateway" {
  description = "Create API Gateway, Lambda authorizer, VPC link and internal NLB"
  type        = bool
  default     = false
}

variable "jwt_secret_arn" {
  description = "ARN of a Secrets Manager secret containing the same raw JWT secret as the application"
  type        = string
  default     = ""
  validation {
    condition     = !var.enable_gateway || startswith(var.jwt_secret_arn, "arn:")
    error_message = "A Secrets Manager secret ARN is required when the gateway is enabled."
  }
}

variable "jwt_issuer" {
  type    = string
  default = "garage-flow"
}

variable "enable_codebuild_runner" {
  description = "Create an ephemeral CodeBuild runner for the protected deployment workflow"
  type        = bool
  default     = false
}

variable "github_connection_arn" {
  description = "Previously authorized CodeConnections GitHub App connection ARN"
  type        = string
  default     = ""
  validation {
    condition     = !var.enable_codebuild_runner || startswith(var.github_connection_arn, "arn:")
    error_message = "An authorized GitHub connection ARN is required for the runner."
  }
}

variable "github_deploy_actor_id" {
  description = "Numeric GitHub user ID authorized to trigger the deploy runner"
  type        = string
  default     = ""
  validation {
    condition     = !var.enable_codebuild_runner || can(regex("^[0-9]+$", var.github_deploy_actor_id))
    error_message = "A numeric GitHub actor ID is required for the runner."
  }
}

variable "terraform_state_bucket" {
  description = "S3 bucket containing Terraform state and its lock file"
  type        = string
  default     = ""
  validation {
    condition     = !var.enable_codebuild_runner || length(var.terraform_state_bucket) > 0
    error_message = "The Terraform state bucket is required when CodeBuild runners are enabled."
  }
}
