# Aggregates

## Customer

Root: `Customer`. Owns document, name, contact fields, active flag, timestamps, and version. Repository boundary: `CustomerRepository`.

## Vehicle

Root: `Vehicle`. Owns plate, brand, model, year, active flag, and customer ownership reference. Repository boundary: `VehicleRepository`.

## CatalogService

Root: `CatalogService`. Owns offered-service name, description, current BRL price, and active flag. Repository boundary: `CatalogServiceRepository`.

## InventoryItem

Root: `InventoryItem`. Owns Part/Supply type, unit price, quantity and active flag. `InventoryMovement` records stock changes. Repository boundary: `InventoryRepository`.

## ServiceOrder

Root: `ServiceOrder`. Owns customer/vehicle snapshots, `ServiceOrderItem`, `Quotation`, `Approval`, `StatusHistoryEntry`, lifecycle status, and customer tracking-token hash.

Invariants:

- At least one service item is required.
- Quotations are immutable snapshots.
- Approval binds to the current quotation version.
- Execution requires an approved current quotation.
- Finished and delivered orders reject invalid workflow actions.

## Value Objects

`Money`, `Document`, `LicensePlate`, `VehicleYear`, and typed UUID identifiers protect domain concepts from primitive misuse.
