# Modelo de dados e consistência

- **ServiceOrder**: ID, cliente, veículo, data de criação, estado e versão; mantém ciclo de vida e histórico existentes. A listagem usa prioridade do estado, data e ID como desempate.
- **Quotation/Approval**: versão, decisão, data e ator. Apenas a versão vigente pode ser decidida; conflitos não geram efeitos parciais.
- **ExternalDecisionReceipt**: emissor e evento, hash do conteúdo, OS, versão, decisão e resultado; unicidade por emissor e evento. Mesmo evento e conteúdo retorna resultado anterior; mesmo ID com conteúdo distinto gera conflito. Persistir recibo e decisão na mesma transação.
- **StatusNotification**: ID, OS, transição, destino, conteúdo mínimo, estado, tentativas, próxima tentativa e lease. Unicidade por transição e canal; registro inserido na transação da mudança. Worker reserva registros de modo concorrente e atualiza resultado. Não guardar credenciais.

Estados da notificação: pendente → em processamento → enviada; falha recuperável retorna a pendente com atraso, falha permanente ou esgotamento fica falha para investigação e reprocessamento. Uma falha após envio e antes da confirmação pode gerar e-mail duplicado.

Novas tabelas e índices usam migrações Flyway incrementais. Avaliar índice da consulta com plano de execução real; preservar histórico.

## Migração e concorrência

Adicionar migrações com números posteriores às existentes, sem editar migrações já aplicadas. Aplicá-las
uma vez por versão com Flyway antes de liberar tráfego; em múltiplas réplicas, coordenar o rollout e
aguardar migração concluída. Preservar dados de OS e decisões anteriores. Para decisões externas, bloquear
a OS e fazer a inserção do recibo com restrição única na mesma transação da decisão; tratar corrida entre
requisições com a mesma chave por leitura do resultado persistido, sem capturar violação SQL em transação
já abortada. Para mensagens, selecionar e reservar registros com bloqueio de linha e expiração de lease;
enviar fora da transação de negócio e registrar resultado em nova transação. Recuperar leases expirados
após falha de processo. Confirmar comportamento por testes com PostgreSQL real e réplicas concorrentes.
