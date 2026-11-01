# Plano de implementação: Evolução da oficina — fase 2

**Data**: 2026-10-30
**Especificação**: [spec.md](spec.md)
**Constituição**: versão 3.0.0; gate aprovado após emenda autorizada pelo usuário.

## Resumo e contexto técnico

Evoluir o monólito com listagem operacional priorizada, integração autenticada de decisões de orçamento e notificações de status, preservando as regras da fase 1. Implantar a aplicação em Kubernetes com Terraform e CI/CD e preparar a entrega. A base usa Kotlin 2.2.21, Java 17, Spring Boot 4.0.9, Gradle, PostgreSQL 16, Flyway, Docker Compose, JUnit, Testcontainers e Kover. O alvo escolhido é AWS com EKS e RDS PostgreSQL na região Ohio; custos e permissões efetivas precisam ser verificados antes de provisionar.

As APIs atuais oferecem criação administrativa, consulta de status e decisão por token do cliente. A integração externa é separada. A listagem administrativa sem filtro passa a retornar apenas ordens ativas, mudança de comportamento necessária ao enunciado; comunicar consumidores e migrar consultas históricas para filtro explícito por status ou consulta individual. A listagem dedicada `/active` oferece o mesmo resultado. Decisões e alternativas estão em [research.md](research.md).

## Verificação constitucional

| Gate | Decisão |
| --- | --- |
| Domínio e monólito | Manter módulos e dependências voltadas ao domínio; sem novos serviços de negócio. |
| Integridade | Decisão e evento de notificação persistidos atomicamente com a OS; estoque e estado mantêm as regras existentes. |
| Segurança e API | Autenticação de integração distinta; segredos externos; contratos versionados e acesso restrito. |
| Testes | Unidade, integração PostgreSQL, segurança e contratos; preservar gate de cobertura de 80%. |
| Operação | Kubernetes, Terraform, Compose, pipeline, observabilidade e instruções reproduzíveis. |
| Desempenho | Medir listagem paginada com 10.000 OS; alvo inicial p95 abaixo de 500 ms em ambiente documentado. |

Reavaliação após design: os artefatos mantêm os gates; deploy e desempenho requerem validação prática.

## Estrutura e responsabilidades

- `src/main/kotlin/com/example/techchallenge/serviceorder/`: domínio, casos de uso, persistência e controladores de OS.
- `src/main/resources/db/migration/`: migrações incrementais para notificações e deduplicação.
- `src/test/kotlin/`: testes dos fluxos e falhas de integração e da consulta paginada.
- `k8s/`: aplicação, Services, configuração, referências de Secrets e HPA; banco gerenciado no RDS.
- `infra/`: Terraform e documentação de cluster, dependências e banco.
- `.github/workflows/`: pipeline de build, testes, imagem e deploy em runner com acesso ao cluster.
- `docs/` e `README.md`: arquitetura, execução, evidências e entrega.

O escalonamento de nós usa Karpenter com EKS Pod Identity e um NodePool limitado a `t3.small`; o controlador fica no grupo gerenciado. O HPA continua responsável por réplicas. A instalação usa um runner CodeBuild dedicado após aprovação de build em `main`, conforme [operação e custos](../../docs/architecture/phase-2-karpenter.md).

## Sequência

1. Inventariar contratos e testes existentes; registrar compatibilidade e medir consulta atual.
2. Adicionar consulta operacional com filtro de estados, prioridade e desempate por data e ID no banco antes da paginação. Preservar consulta histórica.
3. Acrescentar callback externo autenticado, versionado e idempotente; reutilizar regra de decisão e rejeitar conflito e versão obsoleta.
4. Registrar eventos em outbox na mesma transação e disponibilizar adaptador externo opcional, com tentativas limitadas, observabilidade e reconciliação. O ambiente local usa e-mail/Mailpit para teste; na AWS o canal fica desativado e sob responsabilidade operacional da equipe.
5. Preparar imagem e Compose, readiness/liveness, recursos e conexão com banco. Implementar manifests e HPA, com métricas e persistência.
6. Provisionar EKS, rede e RDS privado com Terraform; documentar ferramentas, estado, variáveis, custos e ciclo de vida. Proteger segredos e estado. Revisar plano de recursos antes de aplicar.
7. Configurar pipeline com build, testes, imagem, deploy de banco, migração controlada e manifests. Falhas interrompem etapas; PRs não confiáveis não recebem segredos.
8. Validar [quickstart.md](quickstart.md), atualizar documentação e collection, registrar execução e produzir vídeo e PDF.

## Design e rastreabilidade

- [research.md](research.md): decisões e alternativas.
- [data-model.md](data-model.md): entidades e transações.
- [contracts/api.md](contracts/api.md): contratos propostos e compatibilidade.
- [quickstart.md](quickstart.md): roteiro de validação e entrega.
- [spec.md](spec.md): FR-001–015 cobrem requisitos e entregáveis do PDF, páginas 3–6.

## Complexidade

A outbox adiciona persistência e processamento para recuperar notificações após confirmar transições; envio direto não garante recuperação. Kubernetes e Terraform são obrigações da fase 2. Limitar réplicas, retentativas e infraestrutura ao necessário.
