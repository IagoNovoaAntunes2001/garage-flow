# Event Storming: Inventory

Actors: Administrative User and ServiceOrder execution use case.

Commands: RegisterPart, RegisterSupply, UpdateInventoryItem, AddStock, RemoveStock, ConsumeStockForServiceOrder, RemoveInventoryItem.

Domain events: InventoryItemRegistered, InventoryItemUpdated, StockAdded, StockRemoved, StockConsumed.

Aggregate: InventoryItem owns stock invariants. InventoryMovement records each quantity change.

Business rules:

- Item type is PART or SUPPLY.
- Quantity cannot become negative.
- ServiceOrder execution consumes outstanding quantities only once.
- Insufficient stock rejects the complete operation and rolls back the transaction.
- Referenced or movement-backed items are deactivated rather than physically deleted.
