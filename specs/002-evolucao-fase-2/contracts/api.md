# Contratos propostos

Os contratos existentes de criação, rastreio e decisões do cliente permanecem em vigor; atualizar a especificação completa em `specs/001-repair-shop-mvp/contracts/openapi.yaml` na implementação.

## Listagem operacional

`GET /api/v1/admin/service-orders/active?page=0&size=20` e a listagem administrativa sem filtro exigem JWT administrativo. Retornam apenas `IN_EXECUTION`, `AWAITING_APPROVAL`, `IN_DIAGNOSIS`, `RECEIVED`, nesta ordem, depois data de criação crescente e ID crescente. Paginação ocorre após filtragem e ordenação. A listagem administrativa com filtro explícito permite consulta histórica. A alteração do comportamento sem filtro deve ser comunicada aos consumidores existentes.

## Decisão externa

`POST /api/v1/integrations/quotation-decisions` exige credencial de integração e transporte seguro. Corpo proposto:

```json
{"eventId":"evento-123","serviceOrderId":"00000000-0000-0000-0000-000000000001","quotationVersion":1,"decision":"APPROVED","reason":null}
```

`decision` aceita `APPROVED` ou `REJECTED`; `reason` é opcional para recusa. Sucesso retorna ID da OS, versão e resultado sem dados sensíveis. Reenvio idêntico retorna mesmo resultado; evento reutilizado com conteúdo diferente, decisão conflitante ou versão obsoleta retorna 409; credencial inválida retorna 401/403; corpo inválido retorna 400; OS inexistente retorna 404. A regra não permite executar serviços sem aprovação válida.

## Notificações

Para cada transição confirmada, a mensagem informa ID da OS e novo status ao contato autorizado. Falhas são observáveis e reprocessáveis, sem alterar a transição. Credenciais e detalhes internos não integram a mensagem.
