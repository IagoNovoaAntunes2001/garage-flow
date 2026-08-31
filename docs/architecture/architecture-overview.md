# Architecture Overview

Garage Flow is a Kotlin/Spring Boot modular monolith. Business capabilities are organized by package: customer, vehicle, catalog, inventory, serviceorder, authentication, metrics, and shared.

Each capability follows domain, application, infrastructure, and api layers. Dependencies point inward: controllers call use cases, use cases coordinate domain/repository ports, and infrastructure implements persistence/security details.

PostgreSQL is used because the system has strongly related transactional data: Customers, Vehicles, Catalog Services, Inventory Items, ServiceOrders, Quotations, Approvals, and lifecycle history. Flyway owns schema evolution and Hibernate validates the result.

Administrative APIs use JWT Bearer authentication. Public customer approval/tracking APIs use opaque customer tokens stored only as SHA-256 hashes.
