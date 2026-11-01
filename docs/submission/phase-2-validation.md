# Fase 2 — matriz de conformidade e evidências

## Cobertura do Tech Challenge

| Item obrigatório | Estado | Evidência |
| --- | --- | --- |
| Clean Code e arquitetura em camadas | Atendido | Monólito modular com domínio, aplicação, infraestrutura e API; dependências descritas no README e no desenho de arquitetura. |
| Testes dos fluxos críticos | Atendido | Testes unitários e de integração no Gradle, cobertura Kover e gates de CI. |
| Abertura e consulta de status da OS | Atendido | Contrato OpenAPI e fluxo real executado pelo API Gateway. |
| Aprovação/recusa externa | Atendido | Endpoint autenticado, versionado e idempotente; duplicação, recusa e conflito validados. |
| Listagem priorizada e exclusão lógica | Atendido | Ordem Execução > Aguardando Aprovação > Diagnóstico > Recebida, mais antigas primeiro e paginação testada; finalizadas/entregues ausentes da lista ativa. |
| Comunicação de status por ferramenta externa | Atendido como capacidade opcional | Outbox transacional e adaptador SMTP testável com Mailpit. E-mail não integra a API de negócio e fica desativado na AWS; a equipe assume o canal externo de comunicação. |
| Dockerfile e Compose | Atendido | Imagem multi-stage, usuário não-root, health check, PostgreSQL e Mailpit local. |
| Kubernetes | Atendido | Deployment, Service, ConfigMaps, exemplo de Secret, SecretProviderClass e HPA em `/k8s`. |
| Terraform para cluster e banco | Atendido | VPC, EKS, RDS, ECR, Gateway, runners, Karpenter e Pod Identity em `/infra`, com estado remoto. |
| CI/CD completo | Atendido | GitHub Actions executa build, testes, cobertura, imagem, Trivy, entrega controlada do RDS, add-ons e manifests no EKS somente em `main`. |
| README e desenho da arquitetura | Atendido | Componentes, infraestrutura, fluxo de deploy e instruções local/Kubernetes/Terraform documentados. |
| Collection completa | Atendido | `docs/api/openapi.json`, gerado pela aplicação e importável no Postman. |
| Vídeo de até 15 minutos | Pendente externo | Roteiro pronto; requer gravação, publicação no YouTube/Vimeo e URL real. |
| PDF do portal | Pendente externo | Rascunho local para consulta; a versão de entrega depende da URL real do vídeo e não será versionada no Git. |
| Acesso de `soat-architecture` | Parcial | O repositório é público e legível sem convite. A API do GitHub recusou convites `read` e `triage` para essa conta com HTTP 422; confirmar a política da conta avaliadora no portal. |

## Evidências disponíveis

- [Repositório do projeto](https://github.com/IagoNovoaAntunes2001/garage-flow) e [lista de tarefas da fase 2](../../specs/002-evolucao-fase-2/tasks.md).
- [Especificação completa das APIs gerada pela aplicação](../api/openapi.json).
- [Arquitetura da aplicação, AWS e deploy](../architecture/phase-2-proposed.md) e [instruções de infraestrutura](../../infra/README.md).
- [Pipeline de build, testes, banco, Karpenter e deploy executada com sucesso](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41312984970).
- [Plano de infraestrutura sem alterações após provisionamento do Karpenter](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41231926147).
- [Teste documentado de criação e consolidação de nó](../architecture/phase-2-karpenter.md). A instância temporária foi encerrada após o teste.
- [Job de planejamento e aplicação controlada do RDS no GitHub Actions](https://github.com/IagoNovoaAntunes2001/garage-flow/actions/runs/41262408554): validado com o banco existente.
- Teste de HPA: carga temporária no endpoint de saúde elevou a CPU de aproximadamente 2% para 341% do pedido, acima do alvo de 70%; o HPA aumentou de 2 para 4 réplicas, que ficaram disponíveis. A carga foi removida e os dois nós gerenciados suportaram o teste; após a janela de estabilização, o HPA reduziu para 2 réplicas e o Deployment ficou 2/2 disponível.
- Teste real no API Gateway: cadastro fictício de cliente, veículo, serviço e peça; abertura e rastreamento da OS; diagnóstico; solicitação de orçamento; aprovação externa idempotente; execução, finalização e entrega. A OS entregue não apareceu na listagem ativa. Os registros fictícios foram preservados para demonstração.
- Teste adicional com cinco OS fictícias: prioridade por estado e antiguidade confirmada em páginas de dois itens; recusa bloqueou execução com HTTP 400 e código `QUOTATION_APPROVAL_REQUIRED`; aprovação conflitante retornou HTTP 409. Essas OS permanecem na base para demonstração.
- Em 31/10/2026, autenticação e consulta administrativa pelo API Gateway responderam HTTP 200. A aplicação conectou ao RDS e a implantação ficou com duas réplicas disponíveis.
- Em 01/11/2026, os quatro segredos de aplicação foram centralizados no AWS Secrets Manager. A aplicação passou a consumi-los diretamente pelo Secrets Store CSI Driver e EKS Pod Identity; o Secret Kubernetes legado foi removido depois do rollout 2/2 saudável.
- Validação independente do RDS em 01/11/2026: Job efêmero autenticado com `garage-flow/database-app` confirmou PostgreSQL 16.13, Flyway 3 e seis OS persistidas. O plano Terraform posterior retornou `No changes` e foi aceito pelo guardrail de banco.
- O rollout da migração de segredos exigiu capacidade temporária e o Karpenter criou um nó adicional automaticamente, mantendo os dois pods anteriores disponíveis durante a troca.
- A pipeline executa testes, cobertura e varredura de imagem; ver o resultado de cada execução para evidências da versão correspondente.

## Reteste final de 01/11/2026

- `./gradlew --no-daemon clean build koverVerify`: PASS em 40 segundos.
- Testes do guardrail Terraform: PASS, cinco casos.
- Testes do Lambda authorizer: PASS, três casos, incluindo falha fechada quando o Secrets Manager falha.
- `terraform fmt -check` e `terraform validate`: PASS.
- `docker compose config --quiet`: PASS.
- Validação server-side dos manifests da aplicação e do Job RDS: PASS.
- Rollout com Secrets Manager: PASS, duas de duas réplicas disponíveis; Flyway validou as três migrações no RDS.
- Persistência após rollout: PASS para uma OS entregue e cinco OS ativas da demonstração.
- API Gateway/Lambda authorizer: credencial ausente retornou HTTP 401 e token inválido retornou HTTP 403.
- Imagem `garage-flow:phase2-final`: Trivy 0.69.3 retornou zero vulnerabilidades CRITICAL no Ubuntu e no JAR.
- Após o rollout, as duas réplicas foram redistribuídas para os nós gerenciados, liberando o nó temporário do Karpenter para consolidação.

## Pendente para a submissão

- Repetir a demonstração de escala do HPA durante a gravação do vídeo.
- Gravar e publicar o [vídeo de até 15 minutos](phase-2-video-script.md), informar sua URL no README e preparar o PDF de submissão com os links exigidos.
- Confirmar com a instituição se o acesso público satisfaz o compartilhamento ou se a conta `soat-architecture` precisa alterar sua política de convites; as tentativas nominais foram recusadas pelo GitHub.
- Consultar os créditos pelo console AWS: a organização bloqueia `billing:GetCredits` para a sessão CLI. A infraestrutura ativa continua gerando custos até o teardown autorizado.
