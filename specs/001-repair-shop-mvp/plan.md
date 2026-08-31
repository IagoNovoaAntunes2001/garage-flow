# Implementation Plan: Automotive Repair Shop MVP

**Branch**: `001-repair-shop-mvp` | **Date**: 2026-08-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-repair-shop-mvp/spec.md`

## Summary

Migrate the existing minimal Java/Gradle Spring Boot application in place to an idiomatic Kotlin/JVM 17
Spring Boot 3.5 modular monolith. Deliver authenticated administrative APIs and restricted customer
tracking/approval APIs for customers, vehicles, service catalog, inventory, service orders, immutable
quotations, approvals, lifecycle enforcement, and derived execution metrics. PostgreSQL and Flyway provide
transactional persistence; domain models remain framework-free; Testcontainers, JUnit 5, and Kover enforce
behavior and coverage; Docker Compose, OpenAPI, observability, security scanning, and required DDD and
delivery documentation complete the Phase 1 MVP.

## Technical Context

**Language/Version**: Kotlin 2.2.21 targeting JVM 17; Java 17 runtime/toolchain. Existing Java bootstrap
and test are migrated and removed after Kotlin replacements pass.

**Primary Dependencies**: Spring Boot 3.5.16; Kotlin JVM/Spring/JPA plugins; Spring Web MVC, Data JPA,
Security, OAuth2 Resource Server/Jose, Validation, Actuator; Jackson Kotlin; Flyway; PostgreSQL JDBC;
springdoc-openapi 2.x; Kover 0.9.8. Gradle Groovy DSL and wrapper 8.14.3 remain the build system.

**Storage**: PostgreSQL; Flyway-versioned relational schema; JPA persistence adapters with Hibernate set to
schema validation; no H2 and no production schema auto-creation.

**Testing**: JUnit 5; plain domain unit tests; MockK/SpringMockK only where useful; Spring Boot integration
tests with Testcontainers PostgreSQL; Spring Security test support; Kover package-level line coverage gate
of at least 80% for critical domain packages.

**Target Platform**: Containerized Linux backend on a Java 17 JRE; local development through Docker Compose
with PostgreSQL and backend services.

**Project Type**: Single deployable backend web service implemented as a modular monolith; no frontend,
microservices, brokers, cloud infrastructure, payment, or scheduling components.

**Performance Goals**: At least 95% of valid primary requests complete within 2 seconds under 50 simultaneous
users and 100,000 persisted business records; paginated collection access; no N+1 query patterns; metrics
derived from indexed lifecycle history.

**Constraints**: Pure domain has no Spring/JPA/HTTP/security dependencies; administrative API requires JWT;
customer tokens must resist enumeration; exact currency arithmetic; non-negative stock under concurrency;
immutable completed-order history; current quotation approval required for execution; critical domain line
coverage at least 80%; secrets supplied externally; only public health details exposed.

**Scale/Scope**: One medium-sized repair-shop location, one inventory pool, BRL currency, 50 concurrent
users, 100,000 persisted records, seven business/support capabilities, six service-order statuses, and one
PostgreSQL database.

## Constitution Check

*GATE: Passed before Phase 0 research and re-checked after Phase 1 design.*

| Constitutional gate | Design evidence | Result |
|---|---|---|
| Domain-Driven Design First | Framework-free aggregates/value objects, capability vocabulary, explicit repository ports, lifecycle invariants | PASS |
| Layered Modular Monolith | One deployable with capability-first domain/application/infrastructure/api packages and inward dependencies | PASS |
| Code Quality and Maintainability | Minimal dependency set, typed identifiers, immutable snapshots, focused named use cases, no speculative distributed patterns | PASS |
| Testing Standards | Domain unit tests, PostgreSQL integration tests, security/API tests, deterministic fixtures, Kover 80% critical-package gate | PASS |
| Security by Design | JWT administrative security, BCrypt, hashed opaque customer tokens, boundary/domain validation, external secrets, Trivy | PASS |
| Consistent REST APIs | Versioned resources, command actions, DTO isolation, shared errors, pagination, OpenAPI contract | PASS |
| Service Order Integrity | Aggregate-owned transitions, quotation versions, approval binding, row-locked stock consumption, transactional orchestration | PASS |
| Observability and Performance | Bounded queries, indexes, performance acceptance target, safe structured logs, lifecycle-derived metric | PASS |
| Reproducible Development Environment | Java 17 wrapper build, multi-stage Dockerfile, Compose, Flyway, `.env.example`, clean-start quickstart | PASS |
| MVP Scope Discipline | Explicit exclusions; no microservices, broker, CQRS infrastructure, event sourcing, cache, gateway, frontend, or cloud components | PASS |

Post-design re-check: The data model, API contract, and validation guide preserve every gate. No
constitutional exception or complexity waiver is required.

## Architectural Decisions

### Capability and Layer Boundaries

- `customer`: Customer aggregate, CPF/CNPJ identity, customer CRUD and lookup.
- `vehicle`: Vehicle aggregate, license-plate identity, ownership validation and vehicle CRUD.
- `catalog`: CatalogService aggregate and priced offered-service CRUD.
- `inventory`: InventoryItem aggregate for Part/Supply plus InventoryMovement and controlled stock actions.
- `serviceorder`: ServiceOrder aggregate, items, quotation versions, approvals, lifecycle, administrative
  order use cases, and customer tracking/approval use cases.
- `authentication`: Administrative identity persistence, BCrypt credential verification, JWT issuance and
  request authentication. It does not leak into business domains.
- `metrics`: Read-only application query deriving average active execution time from status history.
- `shared`: Money, typed-ID support, clock/correlation ports, shared domain errors, API errors, and common
  infrastructure configuration only when genuinely cross-cutting.

Inside a capability, dependencies follow `api -> application -> domain`; infrastructure implements domain
or application ports and is wired from the outside. Cross-capability application orchestration depends on
published ports, not another capability's controllers, Spring Data repositories, or JPA entities.

### Transaction and Concurrency Boundaries

- Create/update/delete actions use one application transaction and enforce database uniqueness plus domain
  validation.
- `CreateServiceOrder` reads current customer, vehicle, catalog, and inventory references, verifies
  ownership/activity, snapshots names/prices, persists the aggregate, and returns the one-time tracking
  token in one transaction.
- `StartExecution`/resume locks the ServiceOrder and affected InventoryItem rows in stable identifier order,
  verifies optimistic versions, current approval, and remaining required quantities, appends movements,
  consumes outstanding stock exactly once, and transitions the order atomically.
- Other lifecycle actions lock/version-check the ServiceOrder aggregate and append history in one transaction.
- Duplicate document/plate conflicts, stale aggregate versions, stale quotation approval, and invalid
  transitions map to stable conflict or business-rule errors without partial effects.

### Security Boundaries

- `/api/v1/auth/token`, `/api/v1/tracking/**`, `/api/v1/customer-approvals/**`, API docs in local profile,
  and liveness/readiness health are the only unauthenticated routes.
- `/api/v1/admin/**` requires a valid Bearer JWT with `ROLE_ADMIN`.
- Authentication failure and insufficient authority use centralized JSON errors; CSRF is disabled only for
  the stateless API; sessions are not created.
- Customer tokens are 256-bit random values, stored as SHA-256 hashes, compared through a repository lookup,
  scoped to one order, and never logged or returned after initial issuance.
- The first admin is conditionally bootstrapped from environment variables with a BCrypt hash generated at
  startup; startup rejects missing/weak JWT secrets outside the test profile.

### API and Persistence Boundaries

- Controllers accept request DTOs and return response DTOs; API mapping is explicit and JPA entities never
  cross the infrastructure boundary.
- `ApiError` contains timestamp, status, error, stable code, message, path, correlation ID, and optional
  field violations; no stack trace is serialized.
- Page size defaults to 20 and is constrained to 1..100. Timestamps are UTC ISO-8601 instants. Monetary
  amounts serialize as decimal strings with currency `BRL`.
- Flyway migration `V1__initial_schema.sql` creates the complete Phase 1 schema, constraints, foreign keys,
  indexes, and admin table. Later schema changes use new migrations; Hibernate uses `ddl-auto=validate`.
- JPA mappings are unidirectional where possible. Order detail queries use explicit fetch/entity graphs or
  projections; list queries never fetch full quotation/history graphs.

## Version Control and Git Workflow

### Version Control Strategy

Use a lightweight Git Flow model with two permanent branches:

- `main` represents stable, deliverable versions of the Tech Challenge.
- `develop` is the integration branch for ongoing development.

Regular feature work MUST NOT be implemented directly on `main` or `develop`. Development branches MUST
originate from `develop`, except urgent post-release hotfixes, which originate from `main`.

Supported branch prefixes are:

- `feature/*` for new functionality.
- `fix/*` for non-production bug fixes.
- `refactor/*` for significant refactoring when a dedicated branch is useful.
- `docs/*` for documentation-only work.
- `chore/*` for configuration and project maintenance.
- `test/*` for testing-specific work that does not naturally belong to a feature branch.
- `release/*` for preparation of deliverable releases.
- `hotfix/*` for urgent corrections originating from `main`.

Branch names MUST describe cohesive business or delivery capabilities. Examples include:

```text
feature/customer-management
feature/vehicle-management
feature/service-catalog
feature/inventory-management
feature/service-order
feature/quotation-approval
feature/authentication
docs/ddd-documentation
docs/architecture-diagrams
chore/kotlin-migration
chore/database-setup
chore/docker-setup
```

Do not create one branch for every Spec Kit task. Related domain, application, persistence, API, and test
tasks SHOULD share a capability branch when they form one reviewable increment. For example, the Customer
entity, repository, use cases, controller, and related tests can belong to
`feature/customer-management`.

### Commits

Use Conventional Commits with the supported types `feat`, `fix`, `refactor`, `test`, `docs`, `chore`,
`build`, and `ci`. Prefer scopes based on business capabilities or a clearly bounded technical concern.

Representative commits are:

```text
build(kotlin): configure Kotlin JVM 17
refactor(kotlin): migrate application from Java to Kotlin
feat(customer): implement customer domain model
feat(customer): add customer registration use case
feat(vehicle): implement vehicle management
feat(inventory): implement stock management
feat(service-order): implement service order aggregate
feat(service-order): enforce lifecycle transitions
feat(quotation): implement quotation calculation
feat(auth): implement JWT authentication
test(service-order): cover lifecycle transitions
test(inventory): cover insufficient stock rules
docs(ddd): document ubiquitous language
docs(architecture): add modular monolith diagram
chore(database): configure PostgreSQL and Flyway
chore(docker): configure local environment
```

Each commit MUST represent one meaningful, working change and MUST NOT be split artificially by file.
Broken code MUST NOT be committed intentionally. Tests for a behavior SHOULD be committed with that
behavior or immediately afterward while the branch remains green.

### Pull Request and Release Flow

Normal development follows:

```text
develop -> feature/<capability> -> Pull Request -> develop
```

Feature branches MUST NOT normally merge directly into `main`. When Phase 1 reaches a deliverable state,
create `release/1.0.0` from `develop`. The release branch is restricted to final fixes, README completion,
documentation review, Docker and Swagger validation, testing, coverage validation, vulnerability analysis,
and submission preparation.

After successful validation:

```text
release/1.0.0 -> main
release/1.0.0 -> develop
```

Create tag `v1.0.0` on the validated `main` release with release meaning `Phase 1 Tech Challenge`. Release
changes MUST also exist in `develop` so subsequent work does not lose release fixes.

### Hotfix Flow

Urgent corrections to an already released version follow:

```text
main -> hotfix/<description> -> main
                              -> develop
```

A completed hotfix MUST be merged into both `main` and `develop` through the normal reviewed process.

### Branch Protection Expectations

GitHub SHOULD be configured with these protections:

- `main` requires pull requests, blocks force pushes, requires successful CI checks when CI exists, and
  does not receive direct development commits.
- `develop` receives changes through pull requests and requires successful CI checks when CI exists.
- Group projects SHOULD require at least one pull-request approval when practical.

These settings are repository administration work and are documented for maintainers; they are not
configured automatically by implementation agents.

### Spec Kit Task Integration

`tasks.md` MUST preserve technical dependency order while grouping tasks into cohesive, reviewable
implementation groups. A group MAY carry a recommended branch annotation, but a branch MUST NOT be forced
for every task. Prefer approximately these implementation branches:

1. `chore/kotlin-migration`
2. `chore/project-foundation`
3. `feature/customer-management`
4. `feature/vehicle-management`
5. `feature/service-catalog`
6. `feature/inventory-management`
7. `feature/service-order`
8. `feature/quotation-approval`
9. `feature/customer-tracking`
10. `feature/authentication`
11. `feature/execution-metrics`
12. `feature/api-quality`
13. `test/integration-suite`, only if remaining integration work does not naturally belong to a feature
14. `chore/docker-setup`
15. `docs/ddd-documentation`
16. `docs/architecture`
17. `docs/security-analysis`

Task generation MAY combine adjacent groups when that produces a more cohesive change, but it MUST NOT
move tasks ahead of their technical prerequisites. Tests developed with a capability remain in that
capability's group; `test/integration-suite` is reserved for cross-capability or final integration gaps.

### Automation Safety

The coding agent MAY modify in-scope files and run local Git inspection commands. Unless the user gives
explicit instructions for the specific action, the agent MUST NOT:

- Push branches.
- Merge pull requests.
- Force push.
- Delete remote branches.
- Create releases.
- Create tags.

The developer remains responsible for deciding when changes are pushed, merged, released, or tagged.

## Implementation Sequence

Testing accompanies each phase and is not deferred.

1. **Foundation and in-place Kotlin migration**: Pin Boot/Kotlin/Gradle versions; add Kotlin, web, JPA,
   security, validation, Flyway, PostgreSQL, OpenAPI, Actuator, test, and Kover dependencies; migrate the
   bootstrap and context test to `src/*/kotlin`; preserve the application name; prove the Kotlin test and
   remove obsolete Java files.
2. **Configuration and local database**: Add profile-aware environment configuration, PostgreSQL datasource,
   Flyway, Hibernate validation, safe logging, Actuator health, `.env.example`, initial Compose PostgreSQL,
   Testcontainers base support, and `V1` migration.
3. **Shared domain**: Implement typed IDs, `Money`, `Document`/CPF/CNPJ, `LicensePlate`, domain errors, time
   port, and their exhaustive unit tests.
4. **Customer capability**: Aggregate and repository port; JPA entity/mapper/repository adapter; create,
   get, find-by-document, update, list, and deactivate/delete use cases; REST DTO/controller; unit and
   PostgreSQL/API tests.
5. **Vehicle capability**: Aggregate and ownership rules; persistence and ports; CRUD/customer-list use
   cases; REST contract; plate/year tests and customer relationship integration tests.
6. **Catalog capability**: CatalogService aggregate with Money; persistence; CRUD/list use cases and API;
   price snapshot regression tests.
7. **Inventory capability**: Part/Supply type, InventoryItem aggregate, movements, stock adjustment and
   controlled consumption ports; row-locking persistence; CRUD/list/stock action APIs; concurrency and
   insufficient-stock tests.
8. **ServiceOrder domain**: Items and snapshots, immutable quotation versions, approval decisions, exact
   status machine, history, additional-repair loop, execution intervals, domain events where useful, and
   comprehensive plain unit tests.
9. **ServiceOrder orchestration/persistence**: JPA mappings and aggregate mapper; create, diagnosis,
   quotation request, approve/reject, start/resume execution, add repairs, finish, deliver, list/detail,
   and customer tracking use cases; transactional inventory integration; PostgreSQL tests.
10. **Authentication and authorization**: Administrator persistence/bootstrap, BCrypt verification, JWT
    encoder/decoder, token endpoint, security chain, authorization tests, and customer-token concealment.
11. **ServiceOrder and metric APIs**: Administrative command endpoints, list/detail responses, restricted
    tracking/approval endpoints, derived average execution-time query, pagination, centralized errors, and
    end-to-end contract/security tests.
12. **OpenAPI and operational packaging**: Annotations/configuration, Bearer scheme, local Swagger UI,
    multi-stage Dockerfile, completed Compose backend/database health checks, and clean-start validation.
13. **Quality/security gates**: Kover package rules and reports, full deterministic suite, performance query
    verification, Trivy procedure and actual finding record, logging review, and dependency minimization.
14. **DDD/architecture documentation**: Ubiquitous Language, actual aggregates/domain diagram, two Event
    Storming documents, state diagram, system context, modular-monolith architecture, and only useful
    sequence diagrams; validate names and transitions against code.
15. **Delivery documentation**: Complete README and Phase 1 checklist, preserve placeholders for participant
    data, note private repository access for `soat-architecture`, and execute every Definition of Done check.

## Project Structure

### Documentation (this feature)

```text
specs/001-repair-shop-mvp/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
├── checklists/
│   └── requirements.md
└── tasks.md                 # Generated by $speckit-tasks, not this plan
```

### Source Code (repository root)

```text
build.gradle
settings.gradle
gradle/wrapper/gradle-wrapper.properties
Dockerfile
docker-compose.yml
.env.example
README.md
src/
├── main/
│   ├── kotlin/com/example/techchallenge/
│   │   ├── TechChallengeApplication.kt
│   │   ├── customer/{domain/{model,repository,exception},application/{usecase,port},infrastructure/persistence,api/{controller,request,response}}/
│   │   ├── vehicle/{domain/{model,repository,exception},application/{usecase,port},infrastructure/persistence,api/{controller,request,response}}/
│   │   ├── catalog/{domain/{model,repository,exception},application/{usecase,port},infrastructure/persistence,api/{controller,request,response}}/
│   │   ├── inventory/{domain/{model,repository,exception},application/{usecase,port},infrastructure/persistence,api/{controller,request,response}}/
│   │   ├── serviceorder/{domain/{model,repository,service,exception},application/{usecase,port},infrastructure/persistence,api/{controller,request,response}}/
│   │   ├── authentication/{application,infrastructure/{persistence,security},api}/
│   │   ├── metrics/{application,infrastructure,api}/
│   │   └── shared/{domain,api/error,infrastructure/{config,observability}}/
│   └── resources/
│       ├── application.properties
│       ├── application-local.properties
│       ├── application-test.properties
│       └── db/migration/V1__initial_schema.sql
└── test/
    ├── kotlin/com/example/techchallenge/
    │   ├── customer/{domain,application,infrastructure,api}/
    │   ├── vehicle/{domain,application,infrastructure,api}/
    │   ├── catalog/{domain,application,infrastructure,api}/
    │   ├── inventory/{domain,application,infrastructure,api}/
    │   ├── serviceorder/{domain,application,infrastructure,api}/
    │   ├── authentication/{application,infrastructure,api}/
    │   ├── metrics/{application,infrastructure,api}/
    │   └── support/
    └── resources/
        └── application-test.properties
docs/
├── architecture/
│   ├── architecture-overview.md
│   ├── system-context.md
│   ├── modular-monolith.md
│   ├── service-order-lifecycle.md
│   └── sequences.md          # Only retained if it clarifies implemented critical flows
├── ddd/
│   ├── ubiquitous-language.md
│   ├── domain-model.md
│   ├── aggregates.md
│   ├── event-storming-service-order.md
│   └── event-storming-inventory.md
├── security/vulnerability-report.md
└── submission/checklist.md
```

Packages shown with braces are created incrementally only when implementation exists; no empty package
directories are committed. The existing `src/main/java` and `src/test/java` trees are removed after their
Kotlin replacements pass.

**Structure Decision**: Preserve the single existing Gradle project and base package, organize the modular
monolith primarily by business capability, and apply four conceptual layers inside each capability.

## Phase 1 Design Outputs

- [research.md](research.md): Resolved version, architecture, security, persistence, consistency, testing,
  operations, and documentation decisions.
- [data-model.md](data-model.md): Aggregate boundaries, fields, constraints, relationships, persistence
  tables, and exact service-order state machine.
- [contracts/openapi.yaml](contracts/openapi.yaml): Versioned administrative, authentication, approval,
  tracking, and metrics API contract.
- [quickstart.md](quickstart.md): Clean-environment build, migration, authentication, business-flow,
  security, coverage, documentation, and scanning validation guide.

## Complexity Tracking

No constitutional violations require justification. The separate persistence models add mapping work but
directly enforce the mandatory domain/infrastructure boundary; they are not an exception.
