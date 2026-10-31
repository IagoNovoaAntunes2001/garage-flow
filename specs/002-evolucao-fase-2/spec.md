# Especificação: Evolução da oficina — fase 2

**Criada**: 2026-10-30
**Estado**: Rascunho
**Fonte**: SOAT - Fase 2 - Tech challenge.pdf, páginas 2–6; especificação da fase 1 em `specs/001-repair-shop-mvp/spec.md`.

## Cenários e testes

### 1. Abrir e acompanhar uma OS (P1)

O atendente abre uma OS com cliente, veículo, serviços e peças e recebe sua identificação única. O cliente acompanha a situação atual por acesso restrito.

**Aceitação**: dados válidos produzem uma identificação única; dados inválidos ou veículo incompatível não geram OS parcial. A consulta informa uma das situações Recebida, Diagnóstico, Aguardando Aprovação, Execução, Finalizada ou Entregue, sem revelar dados não autorizados.

### 2. Receber decisão do orçamento (P1)

Uma integração externa comunica a aprovação ou recusa do orçamento do cliente.

**Aceitação**: uma decisão autenticada para o orçamento vigente é registrada com data e resultado; aprovação permite o fluxo autorizado e recusa não autoriza execução. Reenvios não duplicam efeitos; decisões inválidas, obsoletas ou não autorizadas são rejeitadas sem alterar a OS.

### 3. Priorizar ordens abertas (P1)

O atendente consulta as ordens em andamento para priorizar o trabalho.

**Aceitação**: a listagem apresenta Execução, Aguardando Aprovação, Diagnóstico e Recebida nessa ordem; dentro de cada situação, as mais antigas aparecem primeiro. Finalizada e Entregue não aparecem na listagem operacional, mas permanecem disponíveis para consulta histórica. A ordenação é aplicada antes da paginação.

### 4. Comunicar atualização de status (P2)

O cliente recebe por ferramenta externa, como e-mail, informação sobre a mudança de situação da sua OS.

**Aceitação**: uma transição válida gera comunicação identificando a OS e a nova situação; falha de entrega não inventa uma transição nem expõe dados indevidos, e pode ser identificada e tratada. Repetições não produzem mudanças adicionais no estado da OS.

### 5. Operar e demonstrar a solução (P1)

A equipe provisiona infraestrutura, publica aplicação e banco, acompanha a execução automatizada e demonstra escalabilidade.

**Aceitação**: ambiente local documentado funciona; infraestrutura e aplicação podem ser implantadas seguindo instruções; a entrega automatizada executa build, testes, imagem e implantação; uma demonstração mostra APIs e aumento de réplicas sob carga observável.

### Casos de borda

- Decisões repetidas ou contraditórias, aprovação de versão antiga e falhas de integração não autorizam trabalho indevido.
- Mudanças concorrentes preservam os invariantes de estoque, orçamento e ciclo de vida da fase 1.
- Empates na data de abertura têm desempate determinístico; a paginação não altera a prioridade global.
- Segredos não aparecem em repositório, respostas, logs ou documentação; dados históricos não são excluídos fisicamente da listagem.
- Uma falha na notificação é observável e não reverte silenciosamente uma transição já confirmada.

## Requisitos

### Requisitos funcionais e de entrega

- **FR-001**: Preservar os fluxos e regras aplicáveis da fase 1 e refatorar com nomes claros, coesão, simplicidade e separação de dependências.
- **FR-002**: Abrir OS com cliente, veículo, serviços e peças, retornando identificação única.
- **FR-003**: Disponibilizar consulta protegida da situação atual com os seis estados definidos.
- **FR-004**: Receber notificações externas de aprovação e recusa, associadas à versão vigente do orçamento, com autenticação e tratamento idempotente.
- **FR-005**: Listar OS abertas na prioridade Execução > Aguardando Aprovação > Diagnóstico > Recebida, com mais antigas primeiro dentro de cada estado e paginação estável.
- **FR-006**: Excluir logicamente Finalizada e Entregue da listagem operacional, preservando histórico e consulta individual autorizada.
- **FR-007**: Comunicar mudanças de status ao cliente por meio de ferramenta externa; registrar resultado e permitir tratamento de falhas.
- **FR-008**: Cobrir fluxos críticos com testes automatizados unitários e/ou de integração.
- **FR-009**: Manter Dockerfile e docker-compose para execução local.
- **FR-010**: Disponibilizar em `/k8s` manifestos de Deployments, Services, ConfigMaps, Secrets e HPA baseado em CPU e/ou memória.
- **FR-011**: Disponibilizar em `/infra` Terraform para provisionar cluster Kubernetes local ou em nuvem e banco de dados, documentando recursos e aplicação.
- **FR-012**: Configurar pipeline que execute build, testes, build da imagem, deploy no cluster e do banco, e aplicação dos manifestos.
- **FR-013**: Atualizar README com objetivos, arquitetura dos componentes e infraestrutura, fluxo de deploy, instruções local/Kubernetes/Terraform e link para collection completa das APIs.
- **FR-014**: Disponibilizar link de vídeo público ou não listado no YouTube ou Vimeo, com até 15 minutos, demonstrando deploy, CI/CD, consumo das APIs e escalabilidade automática.
- **FR-015**: Preparar PDF de submissão com link do repositório compartilhado com `soat-architecture`, desenho da arquitetura e link do vídeo.
- **FR-016**: Disponibilizar API Gateway na AWS com integração privada por VPC Link e NLB interno para o serviço no EKS, quando habilitado.
- **FR-017**: Usar Lambda authorizer nas rotas administrativas, validando o JWT existente; manter autenticação e autorização da aplicação e preservar o acesso específico das rotas de clientes e integração externa.
- **FR-018**: Antes de provisionar, verificar elegibilidade do plano e créditos, estimar custos e documentar remoção dos recursos, preservação de dados e eventuais custos remanescentes.

### Entidades principais

- **OS**: identificação, cliente, veículo, itens, data de abertura, situação e histórico.
- **Orçamento e decisão**: versão, valores e decisão externa vinculada à versão.
- **Notificação de status**: OS, transição, destinatário, resultado e tentativas.

## Critérios de sucesso

- **SC-001**: Todos os seis estados são consultáveis e os quatro estados abertos seguem a prioridade e ordenação exigidas em testes, inclusive entre páginas.
- **SC-002**: Todos os cenários de aprovação, recusa, repetição e decisão obsoleta são verificáveis sem duplicar efeitos.
- **SC-003**: Um fluxo de abertura até entrega é demonstrável, incluindo comunicação de status e preservação do histórico.
- **SC-004**: A pipeline executa todas as etapas exigidas e a demonstração registra alteração de réplicas em resposta à carga.
- **SC-005**: README, arquivos de infraestrutura, collection, vídeo de até 15 minutos e PDF de submissão cobrem todos os itens exigidos nas páginas 4–6 do enunciado.

## Premissas

- A prioridade é agrupada por status e depois por antiguidade; o PDF não especifica desempate, portanto usa-se identificador estável.
- A solução escolhe e documenta um canal de comunicação e mecanismo autenticado de integração; o PDF não impõe fornecedor.
- Recursos reais, credenciais, vídeo e compartilhamento do repositório dependem de configuração e ações da equipe.
- A constituição foi atualizada para permitir a infraestrutura exigida, preservando o monólito e os invariantes de domínio.
