# Infraestrutura AWS — proposta inicial

O Terraform descreve VPC em duas zonas, sub-redes privadas e públicas, um NAT gateway, EKS com dois nós gerenciados, RDS PostgreSQL privado e senha mestra gerenciada pela AWS, e repositório ECR. A opção padrão de RDS é Single-AZ e um único NAT é um ponto de falha; habilitar Multi-AZ e redundância de rede exige custo adicional. O período de backup automático padrão é de um dia (`db_backup_retention_days`); sete dias foram recusados nesta conta gratuita, e esse valor foi provisionado e validado. Zero desativa backups automáticos e recuperação pontual. A configuração econômica foi validada no ambiente de demonstração e não deve ser tratada como topologia de produção altamente disponível.

O módulo EKS está configurado com `enable_irsa = false`, pois esta conta bloqueou provedores OIDC do IAM.
As credenciais de workloads usam EKS Pod Identity, sem depender de OIDC do IAM. Uma role específica da
ServiceAccount `garage-flow` pode ler somente os quatro segredos da aplicação; o Secrets Store CSI Driver
monta os valores no pod. A role do node group ainda contém permissões de rede usadas pelo CNI.
Deploy e planejamento usam runners CodeBuild e roles separadas, validados na conta. O Karpenter também usa EKS Pod Identity.

Opcionalmente, `enable_gateway=true` acrescenta API Gateway HTTP, VPC Link, NLB interno, Lambda authorizer e grupos de segurança. O NLB encaminha ao NodePort 30080 do EKS; Terraform associa o target group ao Auto Scaling Group dos nós. O authorizer verifica assinatura HS256, emissor, expiração e papel ADMIN nas rotas administrativas, sem cache. A própria aplicação continua autorizando todas as rotas, incluindo rastreio e aprovação do cliente; a rota padrão do gateway apenas encaminha as demais requisições. O acesso externo ao gateway usa HTTPS; o trecho privado entre gateway e backend usa HTTP e deve ser substituído por TLS se exigido pelo ambiente.

Para habilitar o gateway, criar previamente no Secrets Manager um segredo cujo conteúdo seja exatamente o mesmo valor bruto de `JWT_SECRET` fornecido à aplicação, e informar somente seu ARN em `jwt_secret_arn`. Não colocar o valor em variáveis Terraform, arquivos versionados ou estado. A role da Lambda recebe somente leitura desse segredo; se ele usar chave KMS gerenciada pelo cliente, será necessário conceder também a permissão de descriptografia. Configurar a rotação coordenada em ambos os lados. O gateway e o NLB geram custos adicionais. A autenticação e uma consulta administrativa foram validadas por meio do gateway; rotação coordenada e demais cenários precisam de validação adicional.

Pré-requisitos: Terraform, AWS CLI, acesso AWS autorizado, região e versão Kubernetes suportada, CIDRs de acesso ao endpoint EKS e orçamento aprovados. Antes de provisionar, configurar backend remoto criptografado e controlado para o estado, inicializar com `terraform init`, executar `terraform fmt -check`, `terraform validate` e revisar `terraform plan` e custos. Somente aplicar após revisão explícita. Não versionar arquivos de variáveis, credenciais ou estado.

O bucket do estado precisa existir previamente, com criptografia, versionamento e acesso restrito. Por exemplo, com valores definidos pela equipe e AWS CLI autenticado:

```sh
terraform init -backend-config="bucket=BUCKET_DO_ESTADO" -backend-config="key=garage-flow/terraform.tfstate" -backend-config="region=REGIAO" -backend-config="use_lockfile=true"
terraform fmt -check
terraform validate
terraform plan -var='aws_region=REGIAO' -var='kubernetes_version=VERSAO_SUPORTADA' -var='cluster_admin_role_arn=ARN_DA_ROLE_ADMIN' -var='eks_public_access_cidrs=["SEU_IP/32"]' -out=deployment.tfplan
terraform show deployment.tfplan
terraform apply deployment.tfplan
```

No projeto de demonstração, foi criado manualmente o bucket `garage-flow-tfstate-361659539017-us-east-2` em Ohio, com versionamento, criptografia SSE-S3, bloqueio de acesso público e política que exige HTTPS. A conexão `garage-flow-github` também foi criada separadamente e precisa ser autorizada no console. Esses recursos de bootstrap não são destruídos pelo Terraform deste diretório e devem ser incluídos no encerramento do ambiente.

Para esta conta autenticada com `aws login`, o backend S3 não conseguiu ler diretamente o perfil. Exportar as credenciais como variáveis de ambiente produz credenciais estáticas temporárias: uma operação longa expirou e o estado precisou ser recuperado. Use um perfil com `credential_process` que execute `aws configure export-credentials --profile garage-flow --format process` e permita a renovação durante operações longas. Se estiver usando um arquivo de configuração AWS separado para esse perfil, o processo deve apontar para o arquivo de configuração original ao invocar o AWS CLI. Proteja ambos os arquivos, mantenha a sessão AWS válida e não registre nem versione a saída do processo. Teste a leitura do estado antes de aplicar; em caso de falha de estado, preserve o arquivo de recuperação e confira o lock e a linhagem antes de qualquer nova operação.

Aplicar somente após revisar o plano e autorizar custos. O plano pode conter informações sensíveis e está ignorado pelo Git. Para outro ambiente, usar estado isolado e parâmetros próprios; não reutilizar o estado de outro projeto.

`cluster_admin_role_arn` deve identificar uma role IAM estável, autorizada a administrar o cluster e a chave KMS. Essa role é independente da identidade que executa o Terraform, para impedir alterações de acesso ao alternar entre execução local e CI. Alterar essa role requer revisão cuidadosa do acesso ao cluster e à chave. A aplicação usa a credencial distinta em `garage-flow/database-app`; o usuário mestre gerenciado pelo RDS permanece separado. JWT, bootstrap administrativo e token de integração ficam em `garage-flow/jwt`, `garage-flow/bootstrap-admin` e `garage-flow/integration`. O CSI monta esses valores diretamente, e o deploy remove o antigo Secret Kubernetes após um rollout saudável. O metrics-server é provisionado como add-on do EKS; verificar disponibilidade e métricas antes de testar HPA. Documentar e executar as migrações Flyway e confirmar conectividade privada antes de liberar a aplicação.

O workflow `.github/workflows/ci.yml` executa build e testes nas branches e pull requests. Após um push em `main`, ele só inicia o deploy depois de o CI passar e se `AWS_DEPLOY_ENABLED=true`; mantenha a variável como `false` até validar infraestrutura, credenciais e runner. Depois de ativá-la, reexecute o workflow do push atual de `main`. O deploy usa um runner temporário do CodeBuild e verifica se o commit continua sendo a ponta atual de `main`. A opção Terraform `enable_codebuild_runner` é desativada por padrão. Para habilitá-la, é necessária uma conexão CodeConnections GitHub App previamente autorizada para o repositório (`github_connection_arn`), o ID numérico do usuário autorizado a disparar o deploy (`github_deploy_actor_id`) e o nome do bucket de estado (`terraform_state_bucket`). Confira a [configuração da conexão](https://docs.aws.amazon.com/codebuild/latest/userguide/connections-github-app.html) e valide as permissões e o webhook na conta. Os runners de planejamento, Karpenter e aplicação foram exercitados na AWS.

Configure no repositório a variável `CODEBUILD_PROJECT_NAME` com o nome exato do projeto (`<cluster_name>-deploy`), pois ela é necessária antes da alocação do runner; configure também `AWS_REGION`, `ECR_REPOSITORY_URL`, `RDS_INSTANCE_ID`, `EKS_CLUSTER_NAME`, `KUBECTL_VERSION` (compatível com o cluster) e os secrets exigidos pelo workflow. Proteja o ambiente GitHub `aws` e a branch `main`, restrinja quem pode alterar workflows e confira a entrega do webhook apenas para o ator e workflow esperados. O repositório é público: não execute código de pull requests não confiáveis com esta role. Não copie credenciais da sessão pessoal AWS para o GitHub. Verifique se a imagem do runner fornece Docker e AWS CLI, se a instalação do kubectl funciona e se o runner alcança o endpoint EKS e os serviços necessários. O projeto usa VPC, NAT, modo privilegiado e role com acesso de deploy ao namespace `default`; requer revisão de segurança e gera custos.

No repositório da demonstração, o ambiente `aws` foi criado com uma política de deploy limitada à branch `main` e essas variáveis foram definidas. A branch `main` exige pull request e checks. Os valores de aplicação não são necessários no GitHub: a role Pod Identity busca os segredos no Secrets Manager durante o mount do pod.

A entrega do banco usa um runner CodeBuild dedicado, habilitado por `enable_database_runner`, com ambiente GitHub `aws-database` restrito à `main`. Após build aprovado e com `DATABASE_PROVISION_ENABLED=true`, ele gera um plano completo e só o aplica se o verificador aceitar criação ou alteração do RDS ou do seu grupo de sub-redes; exclusões, substituições, divergências fora do banco e parâmetros incompatíveis com a configuração econômica interrompem a entrega. O estado remoto é bloqueado pelo Terraform; o runner precisa de leitura ampla para o plano e escrita no objeto de estado, portanto deve ser protegido como infraestrutura sensível. Configure `DATABASE_RUNNER_INFRA_ENABLED=true` e `CODEBUILD_DATABASE_PROJECT_NAME` depois de provisionar o runner e antes de habilitar a execução. O job da aplicação aguarda o banco; Flyway aplica as migrações ao iniciar. O administrador inicial já foi criado e o bootstrap voltou a ficar desativado. E-mail é um canal opcional e permanece desabilitado no ambiente AWS por decisão da equipe; o adaptador pode ser validado localmente com Mailpit. O gate de varredura permanece ativo; a última análise local está em `docs/security/phase-2-image-scan.md`.

Ambos os runners usam `BUILD_GENERAL1_SMALL`, sujeito à franquia mensal aplicável do CodeBuild; uso excedente e serviços associados podem consumir créditos. Esse tamanho já foi utilizado para construir e analisar a imagem na conta.

O workflow manual `.github/workflows/terraform-plan.yml` apenas gera um plano para revisão, no ambiente `aws-plan` e usando um projeto CodeBuild e uma role separados do deploy. Configure `CODEBUILD_PLAN_PROJECT_NAME`, `AWS_REGION`, `TF_STATE_BUCKET`, `EKS_KUBERNETES_VERSION`, `CLUSTER_ADMIN_ROLE_ARN`, `EKS_PUBLIC_ACCESS_CIDRS` (lista JSON), `JWT_SECRET_ARN`, `CODECONNECTION_ARN` e `DEPLOY_ACTOR_ID`. A role usa a política gerenciada AWS `ReadOnlyAccess` e permissões de lock no estado; seu escopo de leitura é amplo e precisa ser revisado para produção. O endpoint público do EKS fica limitado aos CIDRs configurados; mantenha-os atualizados. O plano geral permanece somente de leitura; a entrega separada do banco usa as restrições descritas acima. A criação de credenciais específicas da aplicação em um RDS novo continua sendo uma etapa de bootstrap, e o runner não remove banco ou snapshots.

Para deploy manual, com uma imagem já publicada, os quatro segredos no Secrets Manager, a associação Pod Identity aplicada pelo Terraform e acesso ao cluster configurado:

```sh
aws eks update-kubeconfig --region REGIAO --name NOME_DO_CLUSTER
helm repo add secrets-store-csi-driver https://kubernetes-sigs.github.io/secrets-store-csi-driver/charts
helm repo add aws-secrets-manager https://aws.github.io/secrets-store-csi-driver-provider-aws
helm upgrade --install csi-secrets-store secrets-store-csi-driver/secrets-store-csi-driver --version 1.6.1 --namespace kube-system --set 'tokenRequests[0].audience=sts.amazonaws.com' --set 'tokenRequests[1].audience=pods.eks.amazonaws.com' --wait
helm upgrade --install secrets-provider-aws aws-secrets-manager/secrets-store-csi-driver-provider-aws --version 3.1.4 --namespace kube-system --set secrets-store-csi-driver.install=false --wait
kubectl apply -f ../k8s/service-account.yaml -f ../k8s/secret-provider-class.yaml
kubectl apply -f ../k8s/configmap.yaml -f ../k8s/runtime-config.yaml -f ../k8s/service.yaml -f ../k8s/hpa.yaml
kubectl create configmap garage-flow-runtime --from-literal='DATABASE_URL=jdbc:postgresql://ENDPOINT_RDS:5432/garageflow' --dry-run=client -o yaml | kubectl apply -f -
kubectl set image --local -f ../k8s/deployment.yaml backend=IMAGEM_PUBLICADA -o yaml | kubectl apply -f -
kubectl rollout status deployment/garage-flow
kubectl top pods
kubectl port-forward service/garage-flow 8080:80
```

O `k8s/secret.yaml.example` documenta as chaves esperadas para ambientes sem integração AWS, mas não é aplicado na demonstração. O Service é interno; a exposição externa ocorre pelo NLB, VPC Link e API Gateway. O HPA considera 70% da CPU e 125% da memória solicitada pelos pods (640 MiB para a solicitação atual de 512 MiB), com duas a quatro réplicas; ajustar os alvos após medir a carga. A escala depende das métricas e de capacidade suficiente nos nós.

Para validar o RDS sem passar pela API, aplique `k8s/demo/rds-validation.yaml`. O Job usa a mesma Pod Identity e credencial de aplicação, executa apenas consultas de versão, Flyway e contagem de OS e expira automaticamente após cinco minutos. Em 01/11/2026 ele retornou PostgreSQL 16.13, migração 3 e seis OS persistidas; o plano Terraform subsequente ficou sem mudanças e foi aceito pelo guardrail do banco.

Há cobrança por EKS, nós EC2, NAT, RDS, armazenamento, segredos e tráfego. Planejar remoção após a demonstração sem excluir dados necessários: a proteção de exclusão do RDS e o snapshot final exigem decisão explícita. Nenhum recurso é criado apenas por adicionar estes arquivos.

## Encerramento do ambiente

Antes de remover recursos, confirmar quais dados e artefatos precisam ser preservados e revisar um plano de destruição. Não remover recursos de outras aplicações. A proteção de exclusão do RDS precisa ser alterada deliberadamente antes da remoção; o snapshot final também pode gerar custo. O bucket e o estado remoto, o segredo JWT criado fora deste Terraform, snapshots, logs e imagens retidas podem permanecer e continuar gerando cobrança: inventariar cada um e decidir sua retenção separadamente. Só executar a remoção após revisão e autorização explícitas, e conferir novamente os recursos e custos na AWS após a conclusão.
