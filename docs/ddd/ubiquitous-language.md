# Ubiquitous Language

| Portuguese term | Code term | Definition | Key invariants |
|---|---|---|---|
| Cliente | Customer | Person or company served by the shop. | Exactly one valid CPF/CNPJ; inactive customers cannot start new work. |
| Veiculo | Vehicle | Customer-owned vehicle maintained by the shop. | Valid Brazilian plate; belongs to one active customer for new orders. |
| Servico | CatalogService | Offered repair-shop labor/service. | Active name is unique; price is BRL Money; changes affect only future snapshots. |
| Peca | Part | Inventory item consumed by repairs. | Stock cannot become negative. |
| Insumo | Supply | Consumable inventory item. | Stock cannot become negative. |
| Estoque | Inventory | Available parts and supplies plus movements. | Consumption is explicit and auditable. |
| Ordem de Servico | ServiceOrder | Central workflow for a customer vehicle repair. | Status changes only through named business actions. |
| Orcamento | Quotation | Immutable priced snapshot of selected services/items. | Total = service subtotal + inventory subtotal. |
| Aprovacao | Approval | Customer decision for one quotation version. | Execution requires current approved quotation. |
| Diagnostico | Diagnosis | Workshop analysis step before approval. | Starts from RECEIVED. |
| Execucao | Execution | Active approved repair work. | Consumes outstanding inventory once. |
| Entrega | Delivery | Customer receives the finished vehicle. | Only FINISHED orders can be delivered. |
| Status da OS | ServiceOrderStatus | RECEIVED, IN_DIAGNOSIS, AWAITING_APPROVAL, IN_EXECUTION, FINISHED, DELIVERED. | Invalid transitions are rejected. |
