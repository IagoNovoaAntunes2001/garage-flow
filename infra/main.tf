data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  zones = slice(data.aws_availability_zones.available.names, 0, 2)
  tags  = { Project = var.cluster_name, ManagedBy = "Terraform" }
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "6.7.3"

  name                 = var.cluster_name
  cidr                 = "10.42.0.0/16"
  azs                  = local.zones
  private_subnets      = ["10.42.0.0/20", "10.42.16.0/20"]
  public_subnets       = ["10.42.128.0/24", "10.42.129.0/24"]
  enable_nat_gateway   = true
  single_nat_gateway   = true
  enable_dns_hostnames = true
  public_subnet_tags   = { "kubernetes.io/role/elb" = "1" }
  private_subnet_tags  = { "kubernetes.io/role/internal-elb" = "1" }
  tags                 = local.tags
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "21.25.0"

  name                                     = var.cluster_name
  kubernetes_version                       = var.kubernetes_version
  vpc_id                                   = module.vpc.vpc_id
  subnet_ids                               = module.vpc.private_subnets
  endpoint_public_access                   = true
  endpoint_private_access                  = true
  endpoint_public_access_cidrs             = var.eks_public_access_cidrs
  enable_cluster_creator_admin_permissions = true
  enable_irsa                              = false
  access_entries = var.enable_codebuild_runner ? {
    deploy = {
      principal_arn = aws_iam_role.codebuild_deploy[0].arn
      policy_associations = {
        namespace_admin = {
          policy_arn = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSAdminPolicy"
          access_scope = {
            type       = "namespace"
            namespaces = ["default"]
          }
        }
      }
    }
  } : {}
  addons = {
    coredns        = {}
    kube-proxy     = {}
    vpc-cni        = { before_compute = true }
    metrics-server = {}
  }
  eks_managed_node_groups = {
    application = {
      instance_types = [var.node_instance_type]
      min_size       = 2
      max_size       = 3
      desired_size   = 2
    }
  }
  tags = local.tags
}

resource "aws_security_group" "database" {
  name        = "${var.cluster_name}-database"
  description = "PostgreSQL access from EKS nodes"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }
  tags = local.tags
}

resource "aws_db_subnet_group" "database" {
  name       = "${var.cluster_name}-database"
  subnet_ids = module.vpc.private_subnets
  tags       = local.tags
}

resource "aws_db_instance" "database" {
  identifier                  = "${var.cluster_name}-postgres"
  engine                      = "postgres"
  engine_version              = "16.13"
  instance_class              = var.db_instance_class
  allocated_storage           = 20
  storage_encrypted           = true
  db_name                     = "garageflow"
  username                    = "garage_admin"
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.database.name
  vpc_security_group_ids      = [aws_security_group.database.id]
  publicly_accessible         = false
  multi_az                    = var.db_multi_az
  backup_retention_period     = 7
  deletion_protection         = true
  skip_final_snapshot         = false
  final_snapshot_identifier   = "${var.cluster_name}-final-snapshot"
  tags                        = local.tags
}

resource "aws_ecr_repository" "application" {
  name                 = var.cluster_name
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
  tags = local.tags
}
