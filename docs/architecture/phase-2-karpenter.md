# Karpenter no EKS

O HPA ajusta a quantidade de pods da aplicação; o Karpenter observa pods sem capacidade disponível e cria nós EC2 quando necessário. Dois nós gerenciados existentes hospedam os componentes essenciais e o controlador, que usa uma réplica. Essa configuração é uma demonstração de baixo custo, sem alta disponibilidade para o controlador.

O Terraform cria a identidade do controlador usando EKS Pod Identity, o agente correspondente, um instance profile que reutiliza a role dos nós e etiquetas de descoberta de sub-redes e security group. O NodePool aceita somente instâncias `t3.small` x86 sob demanda e limita o total declarado a 2 vCPU, normalmente um nó adicional. O limite é eventualmente consistente, não um teto de cobrança. Nós vazios são consolidados após dois minutos. A AMI AL2023 está fixada em `v20261026` e deve ser revisada antes de atualizações do EKS. Não há gerenciamento de interrupções Spot nesta configuração.

O controlador é instalado pelo GitHub Actions depois do build aprovado em `main`, em um projeto CodeBuild dedicado. Sua role possui permissão de administrador Kubernetes para instalar CRDs e recursos no cluster; restringir esse privilégio e revisar o modelo de confiança antes de produção. O projeto é separado do runner de deploy da aplicação. O provedor OIDC do GitHub não é necessário: o CodeBuild recebe uma role própria e o controlador usa Pod Identity.

Para habilitar, revise um plano Terraform com `enable_karpenter=true` junto das variáveis do ambiente e aplique-o após publicar a configuração na `main`. Ajuste a variável de repositório `KARPENTER_INFRA_ENABLED=true` para os planos posteriores conservarem a infraestrutura, e `KARPENTER_ENABLED=true` para o job de instalação. Configure `CODEBUILD_KARPENTER_PROJECT_NAME` com o nome do projeto criado; o ambiente GitHub `aws-infrastructure` deve permitir apenas `main`. O deploy da aplicação aguarda a instalação quando ela estiver habilitada.

Para demonstrar o escalonamento, crie temporariamente um Deployment de teste com `nodeSelector: {karpenter.sh/nodepool: garage-flow}` e recursos adequados ao `t3.small`; acompanhe `kubectl get nodeclaims,nodes,pods -A -w`. Exclua o Deployment e confirme que o NodeClaim e o nó adicional desaparecem após a consolidação. O HPA com até quatro réplicas pode caber nos nós existentes, portanto aumento de pods por si só não garante a criação de outro nó.

Para desativar com segurança, remova as cargas de teste, aguarde a remoção dos NodeClaims e nós dinâmicos, remova NodePool e EC2NodeClass, e só então desinstale o Helm. Em seguida altere as duas variáveis de repositório para `false`, revise o plano Terraform com `enable_karpenter=false` e aplique a remoção dos recursos do Karpenter. Não desinstale o controlador enquanto existirem NodeClaims, pois isso pode deixar instâncias órfãs.

Mesmo com elegibilidade Free Tier, EKS, rede, RDS, balanceador e instâncias podem consumir créditos e gerar cobranças. Cada nó adicional também inclui armazenamento EBS; acompanhe Billing e remova os recursos ao término da demonstração. A elegibilidade e os créditos não garantem custo zero.

## Validação na AWS

Em 31/10/2026, na região `us-east-2`, o Terraform provisionou o agente Pod Identity, as roles e o runner dedicado. O plano posterior executado pelo GitHub Actions não encontrou alterações. O [workflow de release](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41231718492) concluiu build, instalação do Karpenter e deploy.

Uma carga temporária selecionou o NodePool e criou uma instância `t3.small` usando a AMI fixada; o nó ficou pronto e o pod foi executado. Após remover a carga e realocar uma réplica da aplicação que havia sido agendada nesse nó durante o rollout, o Karpenter identificou o nó vazio, removeu o NodeClaim e encerrou a instância. Ao final, as duas réplicas da aplicação ficaram saudáveis nos nós gerenciados. O acesso à autenticação e a uma consulta administrativa pelo gateway retornou HTTP 200.
