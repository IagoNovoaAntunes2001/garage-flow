# Tarefas: Evolução da oficina — fase 2

**Fontes**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api.md](contracts/api.md).

## Fase 1: Preparação

- [X] T001 Inventariar os contratos e testes existentes em `src/main/kotlin/com/example/techchallenge/serviceorder/` e `src/test/kotlin/com/example/techchallenge/serviceorder/` e registrar diferenças em `specs/002-evolucao-fase-2/research.md`.
- [X] T002 Verificar ferramentas, exclusões de segredos e estado Terraform em `.gitignore` e `.dockerignore`.

## Fase 2: Fundamentos

- [X] T003 Verificar configurações e autenticação existentes em `src/main/kotlin/com/example/techchallenge/authentication/infrastructure/security/SecurityConfiguration.kt` e `src/main/resources/application.properties`, preservando os contratos atuais.
- [X] T004 Documentar estratégia de migração, inicialização e execução concorrente em `specs/002-evolucao-fase-2/data-model.md`.

## Fase 3: US1 — Abertura e consulta (P1)

**Teste independente**: criar OS válida, consultar os seis estados e verificar acesso indevido e ausência de criação parcial.

- [X] T005 [US1] Cobrir abertura, estados e isolamento do rastreio em `src/test/kotlin/com/example/techchallenge/serviceorder/api/ServiceOrderLifecycleIntegrationTest.kt`.
- [X] T006 [US1] Corrigir lacunas descobertas na criação ou rastreio em `src/main/kotlin/com/example/techchallenge/serviceorder/` preservando contratos existentes. Nenhuma lacuna identificada nos cenários cobertos.

## Fase 4: US2 — Decisão externa (P1)

**Teste independente**: testar aprovação, recusa, duplicação, conflito, versão obsoleta e credencial inválida.

- [X] T007 [US2] Criar testes de contrato e concorrência do callback em `src/test/kotlin/com/example/techchallenge/serviceorder/api/ServiceOrderLifecycleIntegrationTest.kt`.
- [X] T008 [US2] Criar migração incremental de recibos únicos em `src/main/resources/db/migration/`.
- [X] T009 [US2] Implementar persistência e caso de uso idempotente em `src/main/kotlin/com/example/techchallenge/serviceorder/application/` e `src/main/kotlin/com/example/techchallenge/serviceorder/infrastructure/`.
- [X] T010 [US2] Adicionar autenticação de integração e endpoint em `src/main/kotlin/com/example/techchallenge/authentication/infrastructure/security/SecurityConfiguration.kt` e `src/main/kotlin/com/example/techchallenge/serviceorder/api/`.
- [X] T011 [US2] Documentar callback e credenciais em `specs/001-repair-shop-mvp/contracts/openapi.yaml` e `.env.example`.

## Fase 5: US3 — Priorização (P1)

**Teste independente**: consultar múltiplas páginas com todos os estados e datas, verificando prioridade, desempate e histórico.

- [X] T012 [US3] Criar testes de paginação e prioridade em `src/test/kotlin/com/example/techchallenge/serviceorder/api/ServiceOrderLifecycleIntegrationTest.kt`.
- [X] T013 [US3] Implementar ordenação e filtro no banco em `src/main/kotlin/com/example/techchallenge/serviceorder/infrastructure/persistence/` e porta em `src/main/kotlin/com/example/techchallenge/serviceorder/application/port/ServiceOrderPorts.kt`.
- [X] T014 [US3] Expor listagem operacional em `src/main/kotlin/com/example/techchallenge/serviceorder/api/controller/ServiceOrderQueryController.kt` e documentar em `specs/001-repair-shop-mvp/contracts/openapi.yaml`.
- [X] T015 [US3] Medir consulta com 10.000 OS e registrar resultados e plano em `docs/architecture/phase-2-performance.md`.

## Fase 6: US4 — Comunicação de status (P2)

**Teste independente**: confirmar mensagem em SMTP local, falha recuperável, retomada e concorrência sem perder eventos.

- [X] T016 [US4] Criar testes de outbox, transações e recuperação em `src/test/kotlin/com/example/techchallenge/serviceorder/`.
- [X] T017 [US4] Criar migração incremental de outbox em `src/main/resources/db/migration/`.
- [X] T018 [US4] Registrar eventos transacionais e implementar reserva concorrente e retentativas em `src/main/kotlin/com/example/techchallenge/serviceorder/`.
- [X] T019 [US4] Implementar adaptador SMTP configurável e observabilidade em `src/main/kotlin/com/example/techchallenge/serviceorder/infrastructure/` e `src/main/resources/application.properties`.
- [X] T020 [US4] Configurar SMTP local em `docker-compose.yml` e `.env.example`.

## Fase 7: US5 — Operação e demonstração (P1)

**Teste independente**: provisionar ambiente, executar pipeline, acessar API e observar escalabilidade sob carga.

- [X] T021 [US5] Revisar imagem, health checks e ambiente local em `Dockerfile` e `docker-compose.yml`.
- [X] T022 [US5] Criar manifests de aplicação, serviços, configuração e secrets sem valores reais em `k8s/`.
- [X] T023 [US5] Criar HPA e recursos de métricas e limites em `k8s/` e `infra/`.
- [X] T024 [US5] Provisionar cluster e banco persistente com Terraform em `infra/`, segundo ambiente aprovado e sem guardar segredos no repositório.
- [ ] T025 [US5] Configurar build, testes, imagem, banco e deploy em `.github/workflows/` com credenciais protegidas.
- [ ] T026 [US5] Validar instalação, persistência, pipeline e escala e registrar evidências em `docs/submission/`.

## Fase 8: Entrega e validação

- [X] T027 Atualizar README e desenho de arquitetura, infraestrutura e deploy em `README.md` e `docs/architecture/`.
- [X] T028 Atualizar collection e link das APIs em `README.md` e `specs/001-repair-shop-mvp/contracts/openapi.yaml`.
- [ ] T029 Preparar roteiro e evidências do vídeo de até 15 minutos em `docs/submission/`.
- [ ] T030 Preparar PDF de submissão com repositório, arquitetura e vídeo em `docs/submission/` e confirmar compartilhamento com `soat-architecture`.
- [ ] T031 Executar testes, cobertura, validações de segurança e cenários de `specs/002-evolucao-fase-2/quickstart.md` e registrar resultados em `docs/submission/`.
- [X] T032 Resolver os dois achados críticos restantes do Spring MVC, avaliar migração compatível ou correção suportada e repetir varredura em `docs/security/phase-2-image-scan.md` antes de liberar deploy.
- [ ] T033 Provisionar e validar API Gateway, VPC Link e NLB interno definidos em `infra/gateway.tf`, incluindo conectividade e remoção segura.
- [ ] T034 Validar Lambda authorizer de `infra/authorizer/handler.py` no gateway com tokens válidos e inválidos, rotas de clientes e falhas de secrets.
- [ ] T035 Verificar elegibilidade e créditos da conta AWS, estimar custos e documentar remoção e retenção dos recursos em `infra/README.md` antes de aplicar o plano.
- [ ] T036 Validar conexão GitHub App, permissões e filtros do runner temporário CodeBuild para GitHub Actions; substituir a dependência de OIDC no planejamento Terraform com identidade própria e registrar evidências.

- [X] T037 Provisionar identidade Pod Identity, descoberta e runner dedicado do Karpenter com Terraform e plano revisado.
- [X] T038 Instalar Karpenter por GitHub Actions na main, validar NodePool e EC2NodeClass.
- [X] T039 Demonstrar criação e consolidação de um nó adicional e registrar custos e evidências.
- [X] T040 Automatizar a entrega do RDS por runner dedicado no GitHub Actions, bloquear planos destrutivos e validar o fluxo no estado real.

## Dependências e execução

Preparação precede fundamentos; US1, US2 e US3 dependem dos fundamentos. US4 depende da integração transacional com transições existentes; US5 depende da aplicação configurável e do ambiente escolhido. A validação de entrega depende de todas as histórias. T032 foi concluída; a varredura de segurança deve continuar sendo executada antes de cada deploy. Testes precedem alterações de comportamento. Trabalhos em arquivos distintos, como testes de US2 e US3, podem ser preparados em paralelo; não executar simultaneamente edições no mesmo arquivo. O MVP começa com US1, seguido por US2 e US3, antes da demonstração completa.
