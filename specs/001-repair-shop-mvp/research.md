# Phase 0 Research: Automotive Repair Shop MVP

## Existing Project Baseline

**Decision**: Migrate the existing single-module Gradle project in place. Replace the Java bootstrap class
and context test with Kotlin equivalents, retain the `com.example.techchallenge` base package and
`spring.application.name`, and remove the Java files only after the Kotlin context test passes.

**Rationale**: The repository contains a minimal Java 17 Spring Boot application, a Groovy Gradle build,
one production bootstrap class, one context-load test, and one application property. There is no domain
code or configuration to preserve beyond those elements, and a parallel application would violate the
requested migration constraint.

**Alternatives considered**: Creating a new Kotlin project or a second module was rejected because it
would duplicate the application. Converting the Groovy build to Kotlin DSL was rejected as unrelated
migration risk; `build.gradle` and `settings.gradle` remain Groovy.

## Runtime and Build Compatibility

**Decision**: Use Java toolchain 17, Kotlin 2.2.21 targeting JVM 17, Spring Boot 3.5.16, and Gradle wrapper
8.14.3. Apply Kotlin JVM, Spring, and JPA plugins plus Kover 0.9.8.

**Rationale**: The requested platform is Spring Boot 3.x on Java 17. Boot 3.5.16 requires Java 17 and
supports Gradle 8.4 or later in the 8.x line. The existing Boot 4.1.1 and Gradle 9.7.1 are therefore
intentionally replaced. Kotlin's Spring plugin opens proxied Spring types; the JPA plugin supplies JPA
construction support. Kover provides Kotlin/JVM coverage reports and verification rules.

**Alternatives considered**: Keeping Boot 4.1.1 conflicts with the requirement. Gradle 9.7.1 is outside
Boot 3.5's supported build range. Maven conversion has no benefit. Kotlin 2.4.x was rejected because the
stable, established 2.2.21 line is sufficient and reduces compatibility risk.

## Dependency Set

**Decision**: Use Spring Web MVC, Data JPA, Security, OAuth2 Resource Server/Jose, Validation, Actuator,
Flyway, PostgreSQL JDBC, Jackson Kotlin, Kotlin reflection, springdoc-openapi starter webmvc UI 2.x,
JUnit 5, Spring Security Test, MockK, SpringMockK, Testcontainers PostgreSQL/JUnit, and Kover. Use the
Spring Boot dependency-management BOM wherever it supplies versions.

**Rationale**: Each dependency maps directly to an MVP requirement. Spring Security's JWT encoder and
decoder avoid a second JWT library. MockK is included only for application-port isolation tests;
domain tests use real objects.

**Alternatives considered**: WebFlux is unnecessary for the blocking JPA model. H2 is rejected because
integration behavior must match PostgreSQL. Additional mapping, validation, and JWT libraries are not
needed. Spring Modulith is deferred because package discipline and architecture tests can enforce the
small MVP's module boundaries without another runtime abstraction.

## Modular Monolith and Domain Isolation

**Decision**: Keep one deployable Gradle module and organize source by `customer`, `vehicle`, `catalog`,
`inventory`, `serviceorder`, `authentication`, `metrics`, and `shared`. Each capability contains only the
domain, application, infrastructure, and API packages it actually needs. Pure domain models and ports
contain no Spring, HTTP, security, or JPA annotations. Infrastructure owns JPA entities, Spring Data
interfaces, mappers, and adapters.

**Rationale**: This creates enforceable inward dependencies and capability ownership without the build
and operational overhead of multiple deployables. Separate persistence models protect the domain from
framework requirements and prevent API controllers from returning persistence entities.

**Alternatives considered**: A global controller/service/repository package structure obscures business
boundaries. Annotating domain aggregates as JPA entities is simpler initially but couples invariants to
persistence construction and proxy behavior. Multi-project Gradle modules are unnecessary for Phase 1.

## Domain Boundaries and Consistency

**Decision**: Treat `ServiceOrder` as the central aggregate root owning items, immutable quotation
versions, approvals, and status history. Customer, Vehicle, CatalogService, and InventoryItem are
separate aggregate roots referenced by identifiers and captured snapshots. Inventory movements belong
to the InventoryItem consistency boundary. Cross-aggregate operations run in one application transaction.

**Rationale**: The service-order workflow requires atomic quotation, approval, lifecycle, and historical
state. Separate customer/catalog/inventory aggregates avoid large object graphs and bidirectional JPA
relationships. Snapshot values ensure catalog changes cannot rewrite history.

**Alternatives considered**: One repair-shop aggregate is too large and contention-prone. Independent
quotation and approval aggregate roots would make the required workflow consistency harder without a
business need. Event sourcing and CQRS infrastructure exceed MVP scope.

## Identifiers, Money, and Brazilian Documents

**Decision**: Use UUID value types for aggregate identifiers, `Money` as a non-negative `BigDecimal` plus
fixed `BRL` currency rounded to two decimal places with `HALF_EVEN`, a sealed `Document` value type for
normalized CPF/CNPJ values with check-digit validation, and a normalized `LicensePlate` accepting both
legacy `ABC1234` and Mercosur `ABC1D23` formats. Vehicle year accepts 1886 through current year plus one.

**Rationale**: Explicit types prevent identifier mixing and floating-point currency errors. Normalization
before uniqueness checks makes document and plate rules consistent across domain, API, and persistence.

**Alternatives considered**: Primitive strings and decimals invite invalid states. External document
validation libraries are unnecessary because the algorithms are small and stable. Multi-currency support
is outside the one-shop MVP.

## Service Order Workflow

**Decision**: Permit only `RECEIVED -> IN_DIAGNOSIS -> AWAITING_APPROVAL -> IN_EXECUTION -> FINISHED ->
DELIVERED`, plus `IN_EXECUTION -> AWAITING_APPROVAL -> IN_EXECUTION` for additional repairs. Expose named
commands, never generic status assignment. Each transition appends an immutable history entry.

**Rationale**: Named operations make preconditions explicit and keep status changes as consequences of
domain behavior. The additional-repair loop implements the only justified backward-looking transition.

**Alternatives considered**: A generic update-status endpoint allows invalid or unexplained transitions.
A configurable workflow engine is unnecessary for a fixed six-state MVP.

## Quotations, Approval, and Inventory Timing

**Decision**: Each generated quotation is an immutable numbered snapshot. An approval binds to exactly
one quotation version. Starting or resuming execution requires approval of the current version and, in
the same database transaction, locks affected inventory rows and consumes only quantities not previously
consumed for that order. Insufficient stock aborts the entire action. Idempotency is enforced from order
state, consumed quantities, version checks, and optimistic versions.

**Rationale**: This prevents catalog-price drift, stale approval, negative stock, and duplicate consumption.
Row locking serializes competing stock consumers, while optimistic locking detects concurrent service-order
updates.

**Alternatives considered**: Consuming stock at quotation time would reduce availability for unapproved
work. Deferred consumption after execution begins permits work without stock. Distributed locks and
messaging are unnecessary in one PostgreSQL-backed monolith.

## Persistence and Migrations

**Decision**: Use PostgreSQL with Flyway as the only schema source of truth and set Hibernate schema
management to validation. Use normalized unique columns for document and plate, foreign keys, checks for
non-negative values, optimistic version columns, and indexes matching lookup and listing contracts.

**Rationale**: Customers, vehicles, orders, quotations, approvals, and stock have relational and
transactional integrity needs. Database constraints complement domain enforcement and Flyway guarantees
reproducible empty-database startup.

**Alternatives considered**: Hibernate create/update is not auditable. A document database weakens the
relational guarantees needed by the workflow. H2 does not reproduce PostgreSQL semantics.

## Administrative Authentication

**Decision**: Provide `POST /api/v1/auth/token` for an administrator username/password exchange. Store
BCrypt password hashes, issue short-lived signed JWT access tokens with administrator authority, and
validate them through Spring Security's resource-server support. Bootstrap the first administrator from
environment configuration only when no administrator exists. Require a minimum 256-bit externally
provided signing secret outside tests.

**Rationale**: This meets the required JWT flow with standard Spring components and no plain-text stored
password. Bootstrap configuration avoids expanding MVP scope into account administration.

**Alternatives considered**: Hard-coded users and secrets are insecure. A full authorization server,
refresh-token lifecycle, SSO, and self-service accounts are outside scope. A separate JWT library adds
no needed behavior.

## Customer Tracking and Approval Access

**Decision**: Generate a cryptographically random 256-bit opaque access token per service order, return it
only at administrative order creation, and store only its SHA-256 hash. Customer routes accept the token
in a dedicated request header. Tokens can be revoked and expire after delivery plus a configurable
retention period. Invalid tokens return the same not-found response to avoid order enumeration.

**Rationale**: Predictable order identifiers alone cannot expose customer data, the token grants no
administrative authority, and hashing limits damage if storage is exposed.

**Alternatives considered**: Public UUID-only lookup is insufficient authorization. Customer accounts add
identity-management scope. JWT customer tokens are unnecessary because the credential has one narrow,
server-stored capability.

## API Conventions

**Decision**: Version routes under `/api/v1`. Use plural nouns for resources, command subresources for
lifecycle actions, zero-based `page` and bounded `size` pagination, UTC ISO-8601 timestamps, UUID strings,
decimal currency strings, and one `ApiError` shape. Return 400 for malformed input, 401/403 for security,
404 for absent or concealed resources, 409 for uniqueness/concurrency/state conflicts, and 422 for valid
requests that violate business preconditions.

**Rationale**: Stable conventions reduce client ambiguity while command routes preserve domain intent.

**Alternatives considered**: Returning JPA entities leaks persistence. Generic status updates weaken the
domain. Offset/limit and cursor pagination are both valid; page/size is simpler for the MVP administrative
lists.

## Execution Metrics

**Decision**: Calculate active execution time as the sum of every interval from entry into `IN_EXECUTION`
until exit to `AWAITING_APPROVAL` or `FINISHED`. The average for a requested UTC period includes orders
that entered `FINISHED` during the inclusive start/exclusive end period and returns count plus average
duration. Do not persist a mutable aggregate average.

**Rationale**: This definition correctly excludes waits for additional approval and remains reproducible
from immutable lifecycle history.

**Alternatives considered**: First execution-to-finish wall-clock time overstates work when approval is
pending. Manually maintained averages can drift from source events.

## Testing and Coverage

**Decision**: Use plain JUnit 5 tests for domain behavior, MockK for isolated application orchestration
only where a fake is not clearer, and Spring Boot/Testcontainers PostgreSQL tests for migrations,
persistence, security, and REST contracts. Use one reusable PostgreSQL container per test suite with
transactional cleanup or explicit reset. Configure Kover line verification at 80% for critical domain
packages and report overall line/branch coverage.

**Rationale**: The strategy keeps domain tests fast and validates PostgreSQL-specific integration. Package
filters apply the constitutional threshold to business logic rather than generated/configuration code.

**Alternatives considered**: Full-context tests for domain rules are slow and obscure failures. H2 is not
representative. Mocking every collaborator produces brittle interaction tests.

## OpenAPI and Observability

**Decision**: Generate OpenAPI from annotated controllers/DTOs and shared error schemas, configure a Bearer
JWT scheme, and expose Swagger UI only in local/development profiles. Expose only Actuator health publicly;
other actuator endpoints remain disabled or administrator-protected. Use structured key-value logs with
correlation ID, order ID, transition, and actor type, never tokens or full documents.

**Rationale**: This satisfies local API discovery and operational diagnosis without widening production
attack surface.

**Alternatives considered**: Hand-maintaining runtime OpenAPI separately risks drift; the planning contract
remains the design baseline and contract tests will detect drift. Public actuator details expose internals.

## Containers and Configuration

**Decision**: Use a multi-stage Dockerfile that builds with the checked-in Gradle wrapper and runs on a
Java 17 JRE as a non-root user. Docker Compose starts PostgreSQL and the backend with health checks and
environment-driven credentials/secrets. Provide `.env.example`; keep real `.env` ignored.

**Rationale**: A clean checkout has one reproducible startup path and no committed credentials.

**Alternatives considered**: Host-built artifacts make container builds less reproducible. Adding Redis,
brokers, or orchestration platforms has no MVP requirement.

## Vulnerability Scanning

**Decision**: Use Trivy as the single documented scanner for the repository filesystem and built container
image. Record commands, scan date, targets, findings, severity, analysis, and remediation in
`docs/security/vulnerability-report.md`; fail delivery on unresolved critical findings.

**Rationale**: One tool covers dependencies, files, secrets, and the final image with a reproducible local
or CI command and avoids redundant scanners.

**Alternatives considered**: OWASP Dependency-Check focuses on dependencies but not the final container.
Running multiple scanners increases setup and duplicate triage without a Phase 1 benefit.

## Documentation Strategy

**Decision**: Implement the requested `docs/architecture`, `docs/ddd`, `docs/security`, and
`docs/submission` artifacts alongside the corresponding code. Use Mermaid for maintainable diagrams and
validate terms against a single ubiquitous-language glossary. Event Storming documents list actors,
commands, events, aggregates, policies, rules, and read models implemented by the MVP.

**Rationale**: Incremental documentation avoids fictional diagrams and keeps delivery evidence aligned
with actual domain behavior.

**Alternatives considered**: Producing diagrams before implementation details stabilize risks drift.
Binary-only diagram formats are harder to review and version.
