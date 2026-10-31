# Infraestrutura AWS — proposta inicial

O Terraform descreve VPC em duas zonas, sub-redes privadas e públicas, um NAT gateway, EKS com dois nós gerenciados, RDS PostgreSQL privado com backups e senha mestra gerenciada pela AWS, e repositório ECR. A opção padrão de RDS é Single-AZ e um único NAT é um ponto de falha; habilitar Multi-AZ e redundância de rede exige custo adicional. Esta configuração ainda precisa de validação e revisão antes do uso.

O módulo EKS está configurado com `enable_irsa = false`, pois esta conta bloqueou provedores OIDC do IAM.
Isso não configura credenciais para workloads: antes de conceder acesso AWS a pods, avaliar EKS Pod Identity
e proteger as credenciais dos nós. A role do node group ainda contém permissões de rede usadas pelo CNI.
Deploy e planejamento têm configurações alternativas com runners CodeBuild e roles separadas, ainda
dependentes de validação na conta.

Opcionalmente, `enable_gateway=true` acrescenta API Gateway HTTP, VPC Link, NLB interno, Lambda authorizer e grupos de segurança. O NLB encaminha ao NodePort 30080 do EKS; Terraform associa o target group ao Auto Scaling Group dos nós. O authorizer verifica assinatura HS256, emissor, expiração e papel ADMIN nas rotas administrativas, sem cache. A própria aplicação continua autorizando todas as rotas, incluindo rastreio e aprovação do cliente; a rota padrão do gateway apenas encaminha as demais requisições. O acesso externo ao gateway usa HTTPS; o trecho privado entre gateway e backend usa HTTP e deve ser substituído por TLS se exigido pelo ambiente.

Para habilitar o gateway, criar previamente no Secrets Manager um segredo cujo conteúdo seja exatamente o mesmo valor bruto de `JWT_SECRET` fornecido à aplicação, e informar somente seu ARN em `jwt_secret_arn`. Não colocar o valor em variáveis Terraform, arquivos versionados ou estado. A role da Lambda recebe somente leitura desse segredo; se ele usar chave KMS gerenciada pelo cliente, será necessário conceder também a permissão de descriptografia. Configurar a rotação coordenada em ambos os lados. O gateway e o NLB geram custos adicionais; a integração ainda precisa ser validada em AWS, incluindo health checks, rotas, JWT, rotação e acesso privado.

Pré-requisitos: Terraform, AWS CLI, acesso AWS autorizado, região e versão Kubernetes suportada, CIDRs de acesso ao endpoint EKS e orçamento aprovados. Antes de provisionar, configurar backend remoto criptografado e controlado para o estado, inicializar com `terraform init`, executar `terraform fmt -check`, `terraform validate` e revisar `terraform plan` e custos. Somente aplicar após revisão explícita. Não versionar arquivos de variáveis, credenciais ou estado.

O bucket do estado precisa existir previamente, com criptografia, versionamento e acesso restrito. Por exemplo, com valores definidos pela equipe e AWS CLI autenticado:

```sh
terraform init -backend-config="bucket=BUCKET_DO_ESTADO" -backend-config="key=garage-flow/terraform.tfstate" -backend-config="region=REGIAO" -backend-config="use_lockfile=true"
terraform fmt -check
terraform validate
terraform plan -var='aws_region=REGIAO' -var='kubernetes_version=VERSAO_SUPORTADA' -var='eks_public_access_cidrs=["SEU_IP/32"]' -out=deployment.tfplan
terraform show deployment.tfplan
terraform apply deployment.tfplan
```

No projeto de demonstração, foi criado manualmente o bucket `garage-flow-tfstate-361659539017-us-east-2` em Ohio, com versionamento, criptografia SSE-S3, bloqueio de acesso público e política que exige HTTPS. A conexão `garage-flow-github` também foi criada separadamente e precisa ser autorizada no console. Esses recursos de bootstrap não são destruídos pelo Terraform deste diretório e devem ser incluídos no encerramento do ambiente.

Para esta sessão autenticada com `aws login`, o backend S3 não conseguiu ler diretamente o perfil. Uma alternativa local é carregar credenciais temporárias apenas no ambiente do processo com `eval "$(aws configure export-credentials --profile garage-flow --format env)"` antes dos comandos Terraform. Não registrar a saída, gravá-la em arquivo nem enviá-la ao GitHub. Após renovar a sessão, repita o comando no terminal apropriado.

Aplicar somente após revisar o plano e autorizar custos. O plano pode conter informações sensíveis e está ignorado pelo Git. Para outro ambiente, usar estado isolado e parâmetros próprios; não reutilizar o estado de outro projeto.

Depois do provisionamento, configurar acesso autorizado ao cluster para o usuário e a identidade de deploy; a identidade que cria o cluster recebe acesso administrativo. Obter a senha do RDS do Secrets Manager por canal autorizado e configurar o Secret Kubernetes fora do repositório; considerar uma credencial de aplicação distinta do usuário mestre. O metrics-server é provisionado como add-on do EKS; verificar disponibilidade e métricas antes de testar HPA. Documentar e executar as migrações Flyway e confirmar conectividade privada antes de liberar a aplicação.

O workflow `.github/workflows/ci.yml` executa build e testes nas branches e pull requests. Após um push em `main`, ele só inicia o deploy depois de o CI passar e se `AWS_DEPLOY_ENABLED=true`; mantenha a variável como `false` até validar infraestrutura, credenciais e runner. Depois de ativá-la, reexecute o workflow do push atual de `main`. O deploy usa um runner temporário do CodeBuild e verifica se o commit continua sendo a ponta atual de `main`. A opção Terraform `enable_codebuild_runner` é desativada por padrão. Para habilitá-la, é necessária uma conexão CodeConnections GitHub App previamente autorizada para o repositório (`github_connection_arn`), o ID numérico do usuário autorizado a disparar o deploy (`github_deploy_actor_id`) e o nome do bucket de estado (`terraform_state_bucket`). Confira a [configuração da conexão](https://docs.aws.amazon.com/codebuild/latest/userguide/connections-github-app.html) e valide as permissões e o webhook na conta. Os recursos e filtros dos runners ainda não foram testados na AWS.

Configure no repositório a variável `CODEBUILD_PROJECT_NAME` com o nome exato do projeto (`<cluster_name>-deploy`), pois ela é necessária antes da alocação do runner; configure também `AWS_REGION`, `ECR_REPOSITORY_URL`, `RDS_INSTANCE_ID`, `EKS_CLUSTER_NAME`, `KUBECTL_VERSION` (compatível com o cluster) e os secrets exigidos pelo workflow. Proteja o ambiente GitHub `aws` e a branch `main`, restrinja quem pode alterar workflows e confira a entrega do webhook apenas para o ator e workflow esperados. O repositório é público: não execute código de pull requests não confiáveis com esta role. Não copie credenciais da sessão pessoal AWS para o GitHub. Verifique se a imagem do runner fornece Docker e AWS CLI, se a instalação do kubectl funciona e se o runner alcança o endpoint EKS e os serviços necessários. O projeto usa VPC, NAT, modo privilegiado e role com acesso de deploy ao namespace `default`; requer revisão de segurança e gera custos.

No repositório da demonstração, o ambiente `aws` foi criado com uma política de deploy limitada à branch `main` e essas variáveis foram definidas. Isso não protege a própria branch contra alterações: `main` ainda precisa de proteção adequada e os secrets de aplicação ainda precisam ser configurados.

O workflow verifica se o RDS já provisionado está disponível; ainda não provisiona o banco. As migrações são executadas na inicialização da aplicação. O primeiro administrador e a ativação de SMTP precisam ser planejados antes do primeiro deploy; o manifesto mantém bootstrap e notificações desativados por padrão. O gate de varredura permanece ativo; a última análise local está em `docs/security/phase-2-image-scan.md`.

O workflow manual `.github/workflows/terraform-plan.yml` apenas gera um plano para revisão, no ambiente `aws-plan` e usando um projeto CodeBuild e uma role separados do deploy. Configure `CODEBUILD_PLAN_PROJECT_NAME`, `AWS_REGION`, `TF_STATE_BUCKET`, `EKS_KUBERNETES_VERSION`, `EKS_PUBLIC_ACCESS_CIDRS` (lista JSON), `JWT_SECRET_ARN`, `CODECONNECTION_ARN` e `DEPLOY_ACTOR_ID`. A role usa a política gerenciada AWS `ReadOnlyAccess` e permissões de lock no estado; seu escopo de leitura é amplo e precisa ser revisado para produção. O endpoint público do EKS fica limitado aos CIDRs configurados; mantenha-os atualizados. O projeto e o fluxo ainda precisam ser criados e testados. A aplicação do plano e o provisionamento do banco ainda não estão automatizados no CI/CD.

Para deploy manual, com uma imagem já publicada e acesso ao cluster configurado:

```sh
aws eks update-kubeconfig --region REGIAO --name NOME_DO_CLUSTER
kubectl create secret generic garage-flow-secrets --from-env-file=ARQUIVO_LOCAL_DE_SECRETS --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f ../k8s/configmap.yaml -f ../k8s/service.yaml -f ../k8s/hpa.yaml
kubectl set image --local -f ../k8s/deployment.yaml backend=IMAGEM_PUBLICADA -o yaml | kubectl apply -f -
kubectl rollout status deployment/garage-flow
kubectl top pods
kubectl port-forward service/garage-flow 8080:80
```

O arquivo de secrets deve ter as chaves exemplificadas em `k8s/secret.yaml.example`, ser protegido e não ser versionado. O Service é interno; a exposição externa e TLS exigem configuração adicional. O uso do HPA depende das métricas e de capacidade suficiente nos nós.

Há cobrança por EKS, nós EC2, NAT, RDS, armazenamento, segredos e tráfego. Planejar remoção após a demonstração sem excluir dados necessários: a proteção de exclusão do RDS e o snapshot final exigem decisão explícita. Nenhum recurso é criado apenas por adicionar estes arquivos.

## Encerramento do ambiente

Antes de remover recursos, confirmar quais dados e artefatos precisam ser preservados e revisar um plano de destruição. Não remover recursos de outras aplicações. A proteção de exclusão do RDS precisa ser alterada deliberadamente antes da remoção; o snapshot final também pode gerar custo. O bucket e o estado remoto, o segredo JWT criado fora deste Terraform, snapshots, logs e imagens retidas podem permanecer e continuar gerando cobrança: inventariar cada um e decidir sua retenção separadamente. Só executar a remoção após revisão e autorização explícitas, e conferir novamente os recursos e custos na AWS após a conclusão.
