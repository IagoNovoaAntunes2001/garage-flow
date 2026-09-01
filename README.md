# Garage Flow

Garage Flow is an automotive repair shop management API built with Kotlin, Spring Boot, Domain-Driven
Design, and PostgreSQL. The Phase 1 MVP replaces manual notes and spreadsheets with traceable management of
customers, vehicles, services, inventory, ServiceOrders, quotations, approvals, tracking, and execution
metrics.

## Development workflow

Development follows a lightweight Git Flow model integrated with the Spec Kit roadmap:

- `main` contains stable deliverables and `develop` is the integration branch.
- Capability branches originate from an updated `develop` and return through reviewed pull requests.
- Work is grouped by cohesive capabilities such as `feature/customer-management`; there is no branch per
  individual task.
- Behavioral work is test-first. A task is checked in `tasks.md` only after its required validation passes.
- Every branch runs focused tests, the broader test suite where practical, and `./gradlew build` before push.
- Commits follow Conventional Commits and pull requests record tasks, changes, validation, decisions, and
  unresolved issues.
- The implementation coordinator opens the pull request and stops. It never merges its own PR or starts the
  next roadmap branch before a developer confirms the merge.
- Force pushes, destructive cleanup, automatic merges, tags, and releases are prohibited unless explicitly
  authorized.

The complete operational sequence, branch roadmap, release flow, hotfix flow, commit conventions, branch
protection expectations, and automation safety rules are documented in
[Version Control and Implementation Workflow](docs/development/git-workflow.md).

## Current implementation roadmap

Completed work is tracked in
[Spec Kit tasks](specs/001-repair-shop-mvp/tasks.md). The roadmap progresses through Kotlin migration,
project foundation, authentication, customer and vehicle management, catalog, inventory, ServiceOrder and
approval workflows, tracking, metrics, API quality, testing, operations, documentation, security analysis,
and finally `release/1.0.0`.

## Technology baseline

- Kotlin targeting JVM 17
- Spring Boot 3.x with Spring Web, Data JPA, Security, and JWT resource server
- PostgreSQL with Flyway migrations
- JUnit 5 and PostgreSQL Testcontainers
- Gradle and Kover
- Docker and Docker Compose in the operational roadmap

## Business scope

The backend manages customers, vehicles, catalog services, inventory parts/supplies, ServiceOrders,
quotations, customer approvals, restricted tracking, and execution-time metrics. Phase 1 excludes frontend,
mobile, payment, scheduling, microservices, brokers, Kubernetes, and cloud infrastructure.

## Architecture

The application is a Kotlin/JVM 17 Spring Boot modular monolith. Packages are organized by business
capability and layered into domain, application, infrastructure, and api responsibilities. Domain code does
not depend on Spring MVC, Spring Security, JWT, JPA, PostgreSQL, Swagger, or Docker.

PostgreSQL is used because the MVP has strongly related transactional data across Customers, Vehicles,
Services, Parts/Supplies, Inventory, ServiceOrders, Quotations, Approvals, and lifecycle history. Flyway is
the schema source of truth and Hibernate validates the schema.

## Local execution

Required environment variables:

```sh
DATABASE_URL=jdbc:postgresql://localhost:5432/garage_flow
DATABASE_USERNAME=garage_flow
DATABASE_PASSWORD=garage_flow
JWT_SECRET=change-me-to-at-least-32-characters
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change-me-admin-password
```

Run tests:

```sh
./gradlew clean test
./gradlew build
```

Run with Docker Compose:

```sh
cp .env.example .env
docker compose up --build
```

Swagger UI is available in the `local` profile at `/swagger-ui.html`. Health is available at
`/actuator/health`.

## Authentication

Administrative endpoints under `/api/v1/admin/**` require a Bearer JWT from `POST /api/v1/auth/token`.
Customer approval and tracking endpoints use the `X-Service-Order-Token` header and expose only restricted
ServiceOrder information.

## ServiceOrder lifecycle

`RECEIVED -> IN_DIAGNOSIS -> AWAITING_APPROVAL -> IN_EXECUTION -> FINISHED -> DELIVERED`.

Additional repairs can move an executing order back to `AWAITING_APPROVAL` and require a new customer
approval before execution resumes.

## Documentation

- [Architecture](docs/architecture/architecture-overview.md)
- [System Context](docs/architecture/system-context.md)
- [Modular Monolith](docs/architecture/modular-monolith.md)
- [ServiceOrder Lifecycle](docs/architecture/service-order-lifecycle.md)
- [Ubiquitous Language](docs/ddd/ubiquitous-language.md)
- [Aggregates](docs/ddd/aggregates.md)
- [Domain Model](docs/ddd/domain-model.md)
- [ServiceOrder Event Storming](docs/ddd/event-storming-service-order.md)
- [Inventory Event Storming](docs/ddd/event-storming-inventory.md)
- [Vulnerability Report](docs/security/vulnerability-report.md)
- [Submission Checklist](docs/submission/checklist.md)
