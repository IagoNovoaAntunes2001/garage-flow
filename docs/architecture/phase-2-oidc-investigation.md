# Investigação: OIDC e conta AWS gratuita

## Resultado observado

Em 2026-10-30, na conta do projeto com plano gratuito ativo, a chamada de leitura
`iam:ListOpenIDConnectProviders` foi negada explicitamente por uma política de controle da organização,
mesmo usando a role de acesso completo ao projeto. A role não consegue remover uma negação explícita de
SCP. A política específica não pôde ser lida pela sessão atual.

A [documentação AWS das políticas gerenciadas](https://docs.aws.amazon.com/accounts/latest/reference/scps-and-rcps-for-projects.html)
lista uma negação para `iam:*Provider*` no plano gratuito da nova experiência. Essa regra é compatível
com o erro observado, mas não foi possível confirmar diretamente o conteúdo da política efetiva. O
[procedimento de OIDC do GitHub](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
depende de um provedor IAM para `token.actions.githubusercontent.com`. Assim, o fluxo OIDC atualmente
previsto nos workflows não pode ser configurado com a sessão e as restrições verificadas.

Separadamente, consultas de EKS, RDS, VPC e API Gateway funcionaram em `us-east-2` e foram negadas em
`sa-east-1`. Isso sugere Ohio como região do projeto, mas deve ser confirmado no AWS Settings. A escolha
da região padrão no CLI não altera a região permitida para a conta. Não houve criação de recursos.

## Alternativa a validar

O [CodeBuild pode hospedar runners efêmeros de GitHub Actions](https://docs.aws.amazon.com/codebuild/latest/userguide/action-runner.html).
Uma possibilidade é executar os jobs de deploy nesses runners e conceder à execução uma role de serviço
com permissões mínimas, em vez de estabelecer confiança OIDC do GitHub no IAM. A autenticação para
conectar CodeBuild ao repositório, a criação das roles, as políticas efetivas da conta, a conectividade
com o EKS e os custos devem ser verificados antes de considerar essa alternativa funcional. Não copiar a
sessão pessoal do AWS CLI para os secrets do repositório.

A configuração opcional em `infra/codebuild.tf` e o job de deploy após CI bem-sucedido em `main` preparam essa alternativa.
Um segundo runner, definido em `infra/codebuild-plan.tf`, prepara uma identidade distinta para o plano
Terraform. Os dois fluxos precisam ser validados na AWS. A role de planejamento usa a política gerenciada
AWS `ReadOnlyAccess`, que tem escopo de leitura amplo, além das permissões específicas de lock do estado.

A tentativa de criação do projeto de deploy retornou `OAuthProviderException` ao acessar a conexão, apesar
de ela estar `AVAILABLE`. A simulação IAM permitiu as ações `codeconnections` para a role e negou as ações
correspondentes `codestar-connections`. As duas variantes foram incluídas no Terraform como correção a
validar; o erro também pode envolver propagação IAM ou configuração da instalação GitHub.

A sessão atual conseguiu consultar a lista de projetos CodeBuild e roles IAM em Ohio, e o IAM informou
que o role manager está habilitado. Essas leituras não comprovam autorização para criar o projeto, seu
webhook ou as roles necessárias; nenhum desses recursos foi criado. Após autenticação em Ohio, a simulação
de permissões retornou `allowed` para a criação dos principais serviços e `explicitDeny` para o provedor
OIDC. Uma conexão `garage-flow-github` foi criada e aguarda autorização interativa no GitHub. Um plano
de teste, com ARNs fictícios e estado local temporário, listou 92 recursos; não pode ser aplicado como está.

O repositório é público: permitir código de pull requests externos num runner com permissões AWS seria
inseguro. O projeto CodeBuild, o webhook e os workflows precisam limitar os eventos e branches confiáveis,
usar uma role de deploy separada e restrita e proteger o ambiente de deploy. O runner deve ser efêmero.
Build e testes de pull requests podem continuar em runners hospedados pelo GitHub sem credenciais AWS.

O backend S3 foi inicializado e um plano com lock remoto também listou 92 criações. A conexão GitHub foi
autorizada, o segredo JWT foi criado e as branches foram protegidas. O provisionamento ainda precisa ser
reconciliado com o estado recuperado, a política de deploy do Gitflow e os limites da conta.

Na tentativa inicial de provisionamento, credenciais temporárias exportadas expiraram durante a criação
do grupo de nós. O estado mais recente foi recuperado para o S3 após verificar a mesma linhagem, a versão
remota e a ausência de processos Terraform ativos; o lock obsoleto foi removido. A tentativa de RDS falhou
porque o plano gratuito recusou a retenção de sete dias. A configuração agora propõe um dia, ainda sem
validação na conta. O grupo de nós ficou marcado como `tainted` no estado; verificar seu estado real
antes de qualquer novo plano ou substituição. Uma leitura do estado usando `credential_process` funcionou;
a renovação durante uma operação longa ainda precisa ser validada.

O módulo EKS habilita IRSA por padrão e tentaria criar outro provedor IAM OIDC. A configuração do projeto
agora define `enable_irsa = false`; caso um pod precise acessar AWS, avaliar
[EKS Pod Identity](https://docs.aws.amazon.com/eks/latest/userguide/pod-identities.html). Essa escolha
resolve identidade de workloads no EKS, não autentica o GitHub Actions na AWS. As permissões para criar
roles e associações de Pod Identity também precisam ser verificadas.

Até que a identidade do CNI seja isolada, o node group mantém as permissões de rede exigidas pelo CNI na
role do nó. É necessário restringir acesso dos pods às credenciais do nó e planejar uma role dedicada
via Pod Identity; desativar IRSA não configura essas proteções automaticamente.

## Pendências

- Ohio (`us-east-2`) foi aprovada e confirmada como região do projeto pela tela de login da AWS.
- Validar, sem provisionar recursos, requisitos e permissões para o runner CodeBuild e para o EKS sem IRSA.
- Antes de qualquer implantação, estimar consumo dos créditos e revisar o plano Terraform com o usuário.
- Repetir o gate de segurança de imagem em cada deploy, conforme `docs/security/phase-2-image-scan.md`.
