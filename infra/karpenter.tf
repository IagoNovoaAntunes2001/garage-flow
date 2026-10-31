module "karpenter" {
  count   = var.enable_karpenter ? 1 : 0
  source  = "terraform-aws-modules/eks/aws//modules/karpenter"
  version = "21.25.0"

  cluster_name                  = module.eks.cluster_name
  create_node_iam_role          = false
  node_iam_role_arn             = module.eks.eks_managed_node_groups["application"].iam_role_arn
  create_access_entry           = false
  create_instance_profile       = true
  node_iam_role_name            = "${var.cluster_name}-karpenter-node"
  node_iam_role_use_name_prefix = false
  namespace                     = "kube-system"
  service_account               = "karpenter"
  enable_spot_termination       = false
  tags                          = local.tags
}

output "karpenter_instance_profile_name" {
  description = "Instance profile used by Karpenter nodes"
  value       = var.enable_karpenter ? module.karpenter[0].instance_profile_name : null
}
