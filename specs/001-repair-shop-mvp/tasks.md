# Tasks: Automotive Repair Shop MVP

**Input**: Design documents from `/specs/001-repair-shop-mvp/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml), and
[quickstart.md](quickstart.md)

**Tests**: Tests are mandatory for critical business flows. Write each listed test first, confirm it fails
for the expected reason, then implement the corresponding behavior.

**Organization**: Tasks are grouped by user story and technical dependency. Recommended branch names apply
to cohesive task groups, not individual tasks. Branch creation, pushing, merging, releases, and tags remain
developer-controlled actions under the Git workflow in [plan.md](plan.md).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with adjacent tasks because it changes different files and has no dependency
  on incomplete adjacent work.
- **[Story]**: Maps a task to a user story in [spec.md](spec.md).
- Every task names the exact file or directory it changes.

## Phase 1: Setup and In-Place Kotlin Migration

**Purpose**: Preserve the existing Gradle project while establishing the supported Kotlin/JVM 17 build.

**Recommended branch**: `chore/kotlin-migration`

- [X] T001 Update `gradle/wrapper/gradle-wrapper.properties` to Gradle 8.14.3 and verify the wrapper runs on Java 17
- [X] T002 Configure Spring Boot 3.5.16, Kotlin 2.2.21 JVM/Spring/JPA plugins, JVM 17, and Kover 0.9.8 in `build.gradle`
- [X] T003 Add minimal Web, JPA, Security/Jose, Validation, Actuator, Flyway, PostgreSQL, Jackson Kotlin, OpenAPI, JUnit 5, MockK, and Testcontainers dependencies in `build.gradle`
- [X] T004 [P] Migrate the application bootstrap to `src/main/kotlin/com/example/techchallenge/TechChallengeApplication.kt`
- [X] T005 [P] Migrate the context smoke test to `src/test/kotlin/com/example/techchallenge/TechChallengeApplicationTests.kt`
- [X] T006 Run the Kotlin context test and then remove superseded files `src/main/java/com/example/techchallenge/TechChallengeApplication.java` and `src/test/java/com/example/techchallenge/TechChallengeApplicationTests.java`

**Checkpoint**: The original project builds as one Kotlin application on Java 17 with no duplicate Java
implementation.

---

## Phase 2: Foundational Platform and Shared Domain

**Purpose**: Build the database, configuration, security, shared domain types, and API conventions that block
all user stories.

**⚠️ CRITICAL**: No user-story API is complete until this phase passes.

### Project Foundation

**Recommended branch**: `chore/project-foundation`

- [X] T007 Preserve the application name and add environment-driven datasource, Flyway, JPA validation, JWT, pagination, and management settings in `src/main/resources/application.properties`
- [X] T008 [P] Add local-only Swagger and safe actuator configuration in `src/main/resources/application-local.properties`
- [X] T009 [P] Add deterministic test profile settings in `src/test/resources/application-test.properties`
- [X] T010 Create the complete constrained and indexed Phase 1 PostgreSQL schema in `src/main/resources/db/migration/V1__initial_schema.sql`
- [X] T011 [P] Implement reusable PostgreSQL Testcontainers lifecycle support in `src/test/kotlin/com/example/techchallenge/support/PostgreSqlIntegrationTest.kt`
- [X] T012 Verify Flyway applies from empty PostgreSQL and Hibernate validates the schema in `src/test/kotlin/com/example/techchallenge/support/DatabaseMigrationIntegrationTest.kt`

### Shared Domain and API Foundation

**Recommended branch**: `chore/project-foundation`

- [X] T013 [P] Write Money arithmetic, scale, currency, and rejection tests in `src/test/kotlin/com/example/techchallenge/shared/domain/MoneyTest.kt`
- [X] T014 [P] Write CPF and CNPJ normalization/check-digit tests in `src/test/kotlin/com/example/techchallenge/shared/domain/DocumentTest.kt`
- [X] T015 [P] Write Brazilian legacy and Mercosur plate validation tests in `src/test/kotlin/com/example/techchallenge/shared/domain/LicensePlateTest.kt`
- [X] T016 [P] Implement BRL Money with exact BigDecimal arithmetic in `src/main/kotlin/com/example/techchallenge/shared/domain/Money.kt`
- [X] T017 [P] Implement sealed CPF/CNPJ Document value types in `src/main/kotlin/com/example/techchallenge/shared/domain/Document.kt`
- [X] T018 [P] Implement normalized LicensePlate and vehicle-year rules in `src/main/kotlin/com/example/techchallenge/shared/domain/LicensePlate.kt`
- [X] T019 [P] Implement typed UUID identifiers in `src/main/kotlin/com/example/techchallenge/shared/domain/Identifiers.kt`
- [X] T020 [P] Implement shared domain exceptions and stable error codes in `src/main/kotlin/com/example/techchallenge/shared/domain/DomainException.kt`
- [X] T021 [P] Implement injectable Clock and secure token ports in `src/main/kotlin/com/example/techchallenge/shared/domain/TechnicalPorts.kt`
- [X] T022 Implement the standard ApiError DTO and field violations in `src/main/kotlin/com/example/techchallenge/shared/api/error/ApiError.kt`
- [X] T023 Implement centralized validation, domain, persistence, and security exception mapping in `src/main/kotlin/com/example/techchallenge/shared/api/error/GlobalExceptionHandler.kt`
- [X] T024 [P] Implement correlation-ID creation and safe request logging in `src/main/kotlin/com/example/techchallenge/shared/infrastructure/observability/CorrelationIdFilter.kt`
- [X] T025 [P] Implement shared zero-based page/size validation and response metadata in `src/main/kotlin/com/example/techchallenge/shared/api/Pagination.kt`
- [X] T026 Verify standard error bodies, field validation, correlation IDs, and absent stack traces in `src/test/kotlin/com/example/techchallenge/shared/api/GlobalExceptionHandlerIntegrationTest.kt`

### Administrative Authentication Foundation

**Recommended branch**: `feature/authentication`

- [X] T027 [P] Write password verification, token expiry, issuer, and authority tests in `src/test/kotlin/com/example/techchallenge/authentication/application/AuthenticationServiceTest.kt`
- [X] T028 [P] Write token endpoint and protected-route security tests in `src/test/kotlin/com/example/techchallenge/authentication/api/AuthenticationIntegrationTest.kt`
- [X] T029 Implement Administrator model and repository port in `src/main/kotlin/com/example/techchallenge/authentication/application/Administrator.kt`
- [X] T030 Implement Administrator JPA entity, Spring Data repository, and adapter in `src/main/kotlin/com/example/techchallenge/authentication/infrastructure/persistence/AdministratorPersistenceAdapter.kt`
- [X] T031 Implement BCrypt credential verification and environment bootstrap in `src/main/kotlin/com/example/techchallenge/authentication/application/AuthenticationService.kt`
- [X] T032 Implement JWT encoding/decoding and externally configured secret validation in `src/main/kotlin/com/example/techchallenge/authentication/infrastructure/security/JwtService.kt`
- [X] T033 Implement stateless security rules, ROLE_ADMIN protection, and JSON authentication failures in `src/main/kotlin/com/example/techchallenge/authentication/infrastructure/security/SecurityConfiguration.kt`
- [X] T034 Implement the administrator token request/response and endpoint in `src/main/kotlin/com/example/techchallenge/authentication/api/AuthenticationController.kt`
- [X] T035 Run authentication integration tests and verify every `/api/v1/admin/**` route is denied without a valid administrator JWT in `src/test/kotlin/com/example/techchallenge/authentication/api/AdminRouteSecurityIntegrationTest.kt`

**Checkpoint**: The Kotlin application starts against migrated PostgreSQL, shared value objects enforce core
validation, errors are consistent, and administrative routes have working JWT security.

---

## Phase 3: User Story 1 - Manage Repair Shop Records (Priority: P1) 🎯 Foundation MVP

**Goal**: Administrators can securely manage customers, vehicles, offered services, parts, supplies, and
stock with domain validation and historical-safe removal.

**Independent Test**: Create a valid customer, owned vehicle, offered service, Part, and Supply; retrieve,
list, update, and remove them; verify duplicates, invalid documents/plates/prices/years, and invalid stock
are rejected.

### Customer Capability

**Recommended branch**: `feature/customer-management`

- [X] T036 [P] [US1] Write Customer aggregate creation, update, and deactivation tests in `src/test/kotlin/com/example/techchallenge/customer/domain/CustomerTest.kt`
- [X] T037 [P] [US1] Write customer CRUD, document lookup, uniqueness, and referenced-removal integration tests in `src/test/kotlin/com/example/techchallenge/customer/api/CustomerApiIntegrationTest.kt`
- [X] T038 [P] [US1] Implement Customer aggregate and invariants in `src/main/kotlin/com/example/techchallenge/customer/domain/model/Customer.kt`
- [X] T039 [P] [US1] Define Customer repository contract in `src/main/kotlin/com/example/techchallenge/customer/domain/repository/CustomerRepository.kt`
- [X] T040 [US1] Implement Customer JPA entity, mapper, Spring Data repository, and adapter in `src/main/kotlin/com/example/techchallenge/customer/infrastructure/persistence/CustomerPersistenceAdapter.kt`
- [X] T041 [US1] Implement create, get, find-by-document, update, list, and remove Customer use cases in `src/main/kotlin/com/example/techchallenge/customer/application/usecase/CustomerUseCases.kt`
- [X] T042 [US1] Implement Customer request/response DTOs and explicit mappings in `src/main/kotlin/com/example/techchallenge/customer/api/CustomerDtos.kt`
- [X] T043 [US1] Implement secured Customer REST operations from the planning contract in `src/main/kotlin/com/example/techchallenge/customer/api/controller/CustomerController.kt`

### Vehicle Capability

**Recommended branch**: `feature/vehicle-management`

- [X] T044 [P] [US1] Write Vehicle ownership, plate, year, update, and deactivation tests in `src/test/kotlin/com/example/techchallenge/vehicle/domain/VehicleTest.kt`
- [X] T045 [P] [US1] Write vehicle CRUD, plate uniqueness, and customer-list integration tests in `src/test/kotlin/com/example/techchallenge/vehicle/api/VehicleApiIntegrationTest.kt`
- [X] T046 [P] [US1] Implement Vehicle aggregate and ownership invariants in `src/main/kotlin/com/example/techchallenge/vehicle/domain/model/Vehicle.kt`
- [X] T047 [P] [US1] Define Vehicle repository contract in `src/main/kotlin/com/example/techchallenge/vehicle/domain/repository/VehicleRepository.kt`
- [X] T048 [US1] Implement Vehicle JPA entity, mapper, queries, and adapter in `src/main/kotlin/com/example/techchallenge/vehicle/infrastructure/persistence/VehiclePersistenceAdapter.kt`
- [X] T049 [US1] Implement register, get, update, list, customer-list, and remove Vehicle use cases with Customer ownership checks in `src/main/kotlin/com/example/techchallenge/vehicle/application/usecase/VehicleUseCases.kt`
- [X] T050 [US1] Implement Vehicle DTOs and secured REST operations in `src/main/kotlin/com/example/techchallenge/vehicle/api/controller/VehicleController.kt`

### Service Catalog Capability

**Recommended branch**: `feature/service-catalog`

- [X] T051 [P] [US1] Write CatalogService price, update, activity, and snapshot-safety tests in `src/test/kotlin/com/example/techchallenge/catalog/domain/CatalogServiceTest.kt`
- [X] T052 [P] [US1] Write service CRUD, uniqueness, pagination, and validation integration tests in `src/test/kotlin/com/example/techchallenge/catalog/api/CatalogServiceApiIntegrationTest.kt`
- [X] T053 [P] [US1] Implement CatalogService aggregate and repository contract in `src/main/kotlin/com/example/techchallenge/catalog/domain/model/CatalogService.kt`
- [X] T054 [US1] Implement CatalogService JPA mapping, repository, and adapter in `src/main/kotlin/com/example/techchallenge/catalog/infrastructure/persistence/CatalogServicePersistenceAdapter.kt`
- [X] T055 [US1] Implement service catalog CRUD/list use cases in `src/main/kotlin/com/example/techchallenge/catalog/application/usecase/CatalogServiceUseCases.kt`
- [X] T056 [US1] Implement service catalog DTOs and secured REST operations in `src/main/kotlin/com/example/techchallenge/catalog/api/controller/CatalogServiceController.kt`

### Inventory Capability

**Recommended branch**: `feature/inventory-management`

- [X] T057 [P] [US1] Write InventoryItem non-negative stock, adjustment, and movement tests in `src/test/kotlin/com/example/techchallenge/inventory/domain/InventoryItemTest.kt`
- [X] T058 [P] [US1] Write Part/Supply CRUD, stock adjustment, pagination, and insufficient-stock API tests in `src/test/kotlin/com/example/techchallenge/inventory/api/InventoryApiIntegrationTest.kt`
- [X] T059 [P] [US1] Implement Part/Supply InventoryItem aggregate and InventoryMovement rules in `src/main/kotlin/com/example/techchallenge/inventory/domain/model/InventoryItem.kt`
- [X] T060 [P] [US1] Define Inventory repository and ordered-lock contracts in `src/main/kotlin/com/example/techchallenge/inventory/domain/repository/InventoryRepository.kt`
- [X] T061 [US1] Implement InventoryItem and movement JPA mappings with ordered pessimistic-lock query in `src/main/kotlin/com/example/techchallenge/inventory/infrastructure/persistence/InventoryPersistenceAdapter.kt`
- [X] T062 [US1] Implement inventory CRUD, list, add/remove/return stock, and reference-aware removal use cases in `src/main/kotlin/com/example/techchallenge/inventory/application/usecase/InventoryUseCases.kt`
- [X] T063 [US1] Implement inventory DTOs and secured item/adjustment REST operations in `src/main/kotlin/com/example/techchallenge/inventory/api/controller/InventoryController.kt`
- [X] T064 [US1] Verify normalized uniqueness, reference-aware removal, page bounds, and complete US1 authenticated flow in `src/test/kotlin/com/example/techchallenge/records/RepairShopRecordsEndToEndTest.kt`

**Checkpoint**: User Story 1 passes independently and supplies valid master data for service orders.

---

## Phase 4: User Story 2 - Create and Approve a Quotation (Priority: P1) 🎯 Core MVP

**Goal**: Administrators create a ServiceOrder for an owned vehicle, calculate an immutable quotation, and
obtain customer approval through a restricted token flow.

**Independent Test**: Create an order from registered records, confirm exact snapshots and totals, request
approval, approve the current version with the one-time customer token, and prove invalid ownership or
stale approval fails without partial state.

### ServiceOrder Creation

**Recommended branch**: `feature/service-order`

- [X] T065 [P] [US2] Write ServiceOrder creation, ownership, required-service, snapshot, and initial-history tests in `src/test/kotlin/com/example/techchallenge/serviceorder/domain/ServiceOrderCreationTest.kt`
- [X] T066 [P] [US2] Write exact quotation subtotal, total, versioning, and catalog-price isolation tests in `src/test/kotlin/com/example/techchallenge/serviceorder/domain/QuotationTest.kt`
- [X] T067 [P] [US2] Implement ServiceOrder item, party snapshots, and immutable status-history models in `src/main/kotlin/com/example/techchallenge/serviceorder/domain/model/ServiceOrderComponents.kt`
- [X] T068 [P] [US2] Implement immutable Quotation, QuotationLine, state, and exact calculation rules in `src/main/kotlin/com/example/techchallenge/serviceorder/domain/model/Quotation.kt`
- [X] T069 [US2] Implement ServiceOrder aggregate creation and quotation generation behavior in `src/main/kotlin/com/example/techchallenge/serviceorder/domain/model/ServiceOrder.kt`
- [X] T070 [P] [US2] Define ServiceOrder repository and capability lookup ports in `src/main/kotlin/com/example/techchallenge/serviceorder/application/port/ServiceOrderPorts.kt`
- [X] T071 [US2] Implement ServiceOrder, item, quotation, approval, and history JPA mappings and aggregate mapper in `src/main/kotlin/com/example/techchallenge/serviceorder/infrastructure/persistence/ServiceOrderPersistenceAdapter.kt`
- [X] T072 [US2] Implement transactional CreateServiceOrder with customer lookup, vehicle ownership, active-item lookup, price snapshots, and tracking-token hashing in `src/main/kotlin/com/example/techchallenge/serviceorder/application/usecase/CreateServiceOrder.kt`
- [X] T073 [US2] Implement StartDiagnosis and GenerateQuotation/RequestApproval use cases in `src/main/kotlin/com/example/techchallenge/serviceorder/application/usecase/PrepareQuotation.kt`

### Quotation Approval

**Recommended branch**: `feature/quotation-approval`

- [X] T074 [P] [US2] Write approval, rejection, stale-version, revoked-token, and non-disclosure tests in `src/test/kotlin/com/example/techchallenge/serviceorder/application/QuotationApprovalTest.kt`
- [X] T075 [P] [US2] Implement Approval decision model bound to one quotation version in `src/main/kotlin/com/example/techchallenge/serviceorder/domain/model/Approval.kt`
- [X] T076 [US2] Implement cryptographic tracking-token generation, SHA-256 hashing, expiry, and revocation adapter in `src/main/kotlin/com/example/techchallenge/serviceorder/infrastructure/security/CustomerAccessTokenService.kt`
- [X] T077 [US2] Implement customer ApproveQuotation and RejectQuotation use cases with concealed access failures in `src/main/kotlin/com/example/techchallenge/serviceorder/application/usecase/DecideQuotation.kt`
- [X] T078 [US2] Implement ServiceOrder creation, diagnosis, quotation-request, and restricted approval DTOs in `src/main/kotlin/com/example/techchallenge/serviceorder/api/ServiceOrderCommandDtos.kt`
- [X] T079 [US2] Implement secured creation/diagnosis/quotation endpoints and public approve/reject endpoints in `src/main/kotlin/com/example/techchallenge/serviceorder/api/controller/ServiceOrderCommandController.kt`
- [X] T080 [US2] Verify PostgreSQL rollback, historical snapshot persistence, current-version approval, and one-time token return in `src/test/kotlin/com/example/techchallenge/serviceorder/api/ServiceOrderQuotationIntegrationTest.kt`

**Checkpoint**: User Story 2 passes independently through AWAITING_APPROVAL with a recorded decision and
immutable quotation history.

---

## Phase 5: User Story 3 - Execute a Controlled Service Order (Priority: P1) 🎯 Complete Operational MVP

**Goal**: Enforce the exact ServiceOrder lifecycle, approval gates, additional-repair loop, and atomic stock
consumption through delivery.

**Independent Test**: Advance an approved order through execution, add an additional repair requiring a new
approval, resume without double consumption, finish and deliver, then verify invalid transitions and
concurrent insufficient stock leave no partial effects.

**Recommended branches**: `feature/service-order`, followed by `feature/quotation-approval` for the
additional-repair approval increment.

- [X] T081 [P] [US3] Write every valid and invalid ServiceOrder transition test in `src/test/kotlin/com/example/techchallenge/serviceorder/domain/ServiceOrderLifecycleTest.kt`
- [X] T082 [P] [US3] Write pending/rejected/stale approval execution-gate tests in `src/test/kotlin/com/example/techchallenge/serviceorder/domain/ExecutionApprovalTest.kt`
- [X] T083 [P] [US3] Write additional-repair quotation supersession and resume tests in `src/test/kotlin/com/example/techchallenge/serviceorder/domain/AdditionalRepairTest.kt`
- [X] T084 [P] [US3] Write insufficient/concurrent/idempotent inventory consumption tests in `src/test/kotlin/com/example/techchallenge/serviceorder/application/ExecutionInventoryIntegrationTest.kt`
- [X] T085 [US3] Implement exact ServiceOrderStatus transitions and named lifecycle operations in `src/main/kotlin/com/example/techchallenge/serviceorder/domain/model/ServiceOrderLifecycle.kt`
- [X] T086 [US3] Implement additional-repair scope changes, quotation supersession, and approval reset in `src/main/kotlin/com/example/techchallenge/serviceorder/domain/model/ServiceOrder.kt`
- [X] T087 [US3] Implement transactional ordered stock consumption and outstanding-quantity idempotency in `src/main/kotlin/com/example/techchallenge/serviceorder/application/usecase/StartExecution.kt`
- [X] T088 [P] [US3] Implement AddAdditionalRepairs and request-new-approval orchestration in `src/main/kotlin/com/example/techchallenge/serviceorder/application/usecase/AddAdditionalRepairs.kt`
- [X] T089 [P] [US3] Implement FinishServiceOrder and DeliverVehicle orchestration in `src/main/kotlin/com/example/techchallenge/serviceorder/application/usecase/CompleteServiceOrder.kt`
- [X] T090 [US3] Add additional-repair, execution, finish, and delivery business-action routes to `src/main/kotlin/com/example/techchallenge/serviceorder/api/controller/ServiceOrderCommandController.kt`
- [X] T091 [US3] Add safe structured lifecycle, approval, and inventory operation logging in `src/main/kotlin/com/example/techchallenge/serviceorder/application/ServiceOrderAuditLogger.kt`
- [X] T092 [US3] Verify the complete lifecycle, rollback, immutable Finished/Delivered history, and retry safety in `src/test/kotlin/com/example/techchallenge/serviceorder/api/ServiceOrderLifecycleIntegrationTest.kt`

**Checkpoint**: User Story 3 completes the traceable operational workflow from RECEIVED through DELIVERED.

---

## Phase 6: User Story 4 - Track Service Order Progress (Priority: P2)

**Goal**: Customers receive a restricted progress view while administrators can page, filter, and inspect
complete ServiceOrders without inefficient graph loading.

**Independent Test**: Track an order using its valid token, prove all invalid token/order combinations
disclose nothing, then list/filter and inspect complete details with an administrator JWT.

**Recommended branch**: `feature/customer-tracking`

- [X] T093 [P] [US4] Write restricted tracking projection and customer-readable progress tests in `src/test/kotlin/com/example/techchallenge/serviceorder/application/TrackServiceOrderTest.kt`
- [X] T094 [P] [US4] Write missing, wrong, expired, revoked, and cross-order token concealment tests in `src/test/kotlin/com/example/techchallenge/serviceorder/api/CustomerTrackingSecurityIntegrationTest.kt`
- [X] T095 [P] [US4] Write paged status filtering and detailed administrative query-count tests in `src/test/kotlin/com/example/techchallenge/serviceorder/api/ServiceOrderQueryIntegrationTest.kt`
- [X] T096 [US4] Implement bounded summary and optimized detail query projections in `src/main/kotlin/com/example/techchallenge/serviceorder/infrastructure/persistence/ServiceOrderQueryAdapter.kt`
- [X] T097 [P] [US4] Implement customer-safe tracking response mapping in `src/main/kotlin/com/example/techchallenge/serviceorder/api/CustomerTrackingDtos.kt`
- [X] T098 [US4] Implement TrackServiceOrder with hashed-token, expiry, revocation, and order-scope checks in `src/main/kotlin/com/example/techchallenge/serviceorder/application/usecase/TrackServiceOrder.kt`
- [X] T099 [P] [US4] Implement administrative list/detail query use cases in `src/main/kotlin/com/example/techchallenge/serviceorder/application/usecase/QueryServiceOrders.kt`
- [X] T100 [US4] Implement public tracking and secured administrative list/detail endpoints in `src/main/kotlin/com/example/techchallenge/serviceorder/api/controller/ServiceOrderQueryController.kt`
- [X] T101 [US4] Verify bounded pages, no N+1 query pattern, restricted fields, and current progress in `src/test/kotlin/com/example/techchallenge/serviceorder/api/ServiceOrderTrackingEndToEndTest.kt`

**Checkpoint**: User Story 4 passes independently without exposing administrative or unrelated customer data.

---

## Phase 7: User Story 5 - Monitor Execution Time (Priority: P3)

**Goal**: Administrators obtain average active execution time derived from immutable lifecycle history.

**Independent Test**: Calculate the metric for continuous and approval-paused orders in a fixed UTC period
and verify the average, count, interval boundaries, and empty-result semantics.

**Recommended branch**: `feature/execution-metrics`

- [X] T102 [P] [US5] Write active-interval pairing, approval-wait exclusion, range, and empty-set unit tests in `src/test/kotlin/com/example/techchallenge/metrics/application/ExecutionTimeCalculatorTest.kt`
- [X] T103 [P] [US5] Write PostgreSQL lifecycle-history metric query tests in `src/test/kotlin/com/example/techchallenge/metrics/infrastructure/ExecutionTimeQueryIntegrationTest.kt`
- [X] T104 [US5] Implement active execution interval calculation from status history in `src/main/kotlin/com/example/techchallenge/metrics/application/ExecutionTimeCalculator.kt`
- [X] T105 [US5] Implement indexed lifecycle-history query adapter for inclusive-start/exclusive-end completion periods in `src/main/kotlin/com/example/techchallenge/metrics/infrastructure/ExecutionTimeQueryAdapter.kt`
- [X] T106 [US5] Implement the authenticated average execution-time use case and response in `src/main/kotlin/com/example/techchallenge/metrics/application/GetAverageExecutionTime.kt`
- [X] T107 [US5] Implement the secured execution-time endpoint and date-range validation in `src/main/kotlin/com/example/techchallenge/metrics/api/ExecutionMetricsController.kt`
- [X] T108 [US5] Verify continuous, paused, multiple-interval, and no-data metric responses end to end in `src/test/kotlin/com/example/techchallenge/metrics/api/ExecutionMetricsEndToEndTest.kt`

**Checkpoint**: All five user stories are functional and independently verified.

---

## Phase 8: API Quality and Cross-Capability Integration

**Purpose**: Make the implemented runtime contract consistently match the planning contract across stories.

**Recommended branch**: `feature/api-quality`

- [X] T109 [P] Configure springdoc metadata, Bearer JWT security scheme, and local-only Swagger UI in `src/main/kotlin/com/example/techchallenge/shared/infrastructure/config/OpenApiConfiguration.kt`
- [X] T110 [P] Add controller/DTO OpenAPI annotations for authentication, customer, vehicle, catalog, inventory, service-order, tracking, approval, and metrics packages under `src/main/kotlin/com/example/techchallenge/`
- [X] T111 Verify runtime paths, methods, statuses, schemas, security, and shared errors against `specs/001-repair-shop-mvp/contracts/openapi.yaml` in `src/test/kotlin/com/example/techchallenge/shared/api/OpenApiContractIntegrationTest.kt`
- [X] T112 Verify all collection APIs enforce page size 1..100 and stable page metadata in `src/test/kotlin/com/example/techchallenge/shared/api/PaginationIntegrationTest.kt`
- [X] T113 Verify customer/vehicle/catalog/inventory/order cross-capability workflow and transactional consistency in `src/test/kotlin/com/example/techchallenge/support/RepairShopMvpEndToEndTest.kt`

---

## Phase 9: Testing, Coverage, and Performance Gates

**Purpose**: Close cross-capability gaps and enforce the constitutional quality thresholds.

**Recommended branch**: `test/integration-suite` only for gaps not naturally fixed on a capability branch.

- [X] T114 Configure Kover reports and >=80% line verification for critical domain packages in `build.gradle`
- [X] T115 Add missing domain/application tests identified by the Kover report under `src/test/kotlin/com/example/techchallenge/`
- [X] T116 Add repeated clean-run and test-order independence validation guidance to `specs/001-repair-shop-mvp/quickstart.md`
- [X] T117 Create representative fixture generation for 100,000 records in `src/test/kotlin/com/example/techchallenge/support/RepresentativeLoadFixture.kt`
- [X] T118 Verify 50-user primary-flow p95 latency, bounded queries, and query counts in `src/test/kotlin/com/example/techchallenge/performance/RepairShopPerformanceTest.kt`
- [X] T119 Run `clean check koverVerify koverHtmlReport` three consecutive times and record reproducible results in `docs/submission/checklist.md`

---

## Phase 10: Containerization and Operations

**Purpose**: Deliver a reproducible backend/PostgreSQL environment with safe health and configuration.

**Recommended branch**: `chore/docker-setup`

- [X] T120 [P] Create the non-root multi-stage Java 17 backend image in `Dockerfile`
- [X] T121 [P] Create a secrets-free local configuration template in `.env.example`
- [X] T122 Configure backend and PostgreSQL services, dependency health, persistent volume, and environment variables in `docker-compose.yml`
- [X] T123 Configure public liveness/readiness only and protect other actuator endpoints in `src/main/kotlin/com/example/techchallenge/shared/infrastructure/config/ActuatorSecurityConfiguration.kt`
- [X] T124 Add container health, clean Flyway startup, restart persistence, and shutdown validation to `specs/001-repair-shop-mvp/quickstart.md`
- [X] T125 Build and start the environment from a clean database and record the verified commands in `README.md`

---

## Phase 11: DDD and Architecture Documentation

**Purpose**: Document the implemented model and architecture using the same ubiquitous language as code.

### DDD Documentation

**Recommended branch**: `docs/ddd-documentation`

- [X] T126 [P] Document Portuguese mappings, canonical English terms, definitions, and invariants in `docs/ddd/ubiquitous-language.md`
- [X] T127 [P] Document implemented aggregate roots, entities, value objects, repositories, and owned invariants in `docs/ddd/aggregates.md`
- [X] T128 Create the implemented relationship and aggregate-boundary Mermaid diagrams in `docs/ddd/domain-model.md`
- [X] T129 Document ServiceOrder actors, commands, events, aggregates, policies, rules, and read models in `docs/ddd/event-storming-service-order.md`
- [X] T130 Document Part/Supply actors, commands, events, InventoryItem aggregate, policies, and insufficient-stock behavior in `docs/ddd/event-storming-inventory.md`

### Architecture Documentation

**Recommended branch**: `docs/architecture`

- [X] T131 [P] Document layers, dependency direction, persistence, REST, authentication, and PostgreSQL rationale in `docs/architecture/architecture-overview.md`
- [X] T132 [P] Create the Customer, Administrator, backend, and PostgreSQL Mermaid context diagram in `docs/architecture/system-context.md`
- [X] T133 [P] Create the module/component and inward-dependency Mermaid diagrams in `docs/architecture/modular-monolith.md`
- [X] T134 Document every valid command-driven state transition and invalid-transition rule in `docs/architecture/service-order-lifecycle.md`
- [X] T135 Add only useful ServiceOrder creation, approval, and tracking sequence diagrams in `docs/architecture/sequences.md`
- [X] T136 Cross-check every DDD and architecture term, command, event, module, and transition against `src/main/kotlin/com/example/techchallenge/` and correct documentation drift under `docs/`

---

## Phase 12: Security Analysis and Delivery

**Purpose**: Produce reproducible security evidence and complete the Phase 1 delivery material.

### Security Analysis

**Recommended branch**: `docs/security-analysis`

- [X] T137 Document Trivy filesystem/image commands, target, scan date, findings, severity, analysis, and remediation format in `docs/security/vulnerability-report.md`
- [X] T138 Run Trivy against the repository and final image and record actual unsuppressed findings in `docs/security/vulnerability-report.md`
- [X] T139 Resolve critical findings in the owning files or document accepted non-critical risks with accountable remediation in `docs/security/vulnerability-report.md`
- [X] T140 Verify logs and API errors contain no passwords, JWTs, tracking tokens, secrets, full CPF/CNPJ values, or stack traces in `src/test/kotlin/com/example/techchallenge/security/SensitiveDataExposureIntegrationTest.kt`

### README and Submission

**Recommended branch**: `release/1.0.0` after all implementation branches have merged into `develop`

- [X] T141 Complete business problem, scope, stack, Kotlin/Java 17, architecture, DDD, package structure, lifecycle, and PostgreSQL rationale in `README.md`
- [X] T142 Complete environment variables, local/Docker startup, Flyway, tests, coverage, Swagger, authentication, scanning, and documentation links in `README.md`
- [X] T143 Create the complete Phase 1 deliverable checklist with blank group/participant/Discord/link/video fields in `docs/submission/checklist.md`
- [X] T144 Document private repository status and required `soat-architecture` access without changing remote permissions in `docs/submission/checklist.md`
- [X] T145 Execute every scenario in `specs/001-repair-shop-mvp/quickstart.md` and record final validation evidence in `docs/submission/checklist.md`
- [X] T146 Verify implementation terminology and deliverables against `specs/001-repair-shop-mvp/spec.md`, `specs/001-repair-shop-mvp/plan.md`, and `docs/submission/checklist.md`

**Checkpoint**: Phase 1 is eligible for developer-controlled merge from `release/1.0.0` to `main` and
`develop`, followed by developer-controlled tag `v1.0.0`. No agent performs those Git operations without
explicit instruction.

---

## Dependencies and Execution Order

### Phase Dependencies

- **Phase 1 — Setup**: Starts immediately and preserves the existing project.
- **Phase 2 — Foundation**: Depends on Phase 1 and blocks all authenticated business APIs.
- **Phase 3 — US1 Records**: Depends on Phase 2; provides Customer, Vehicle, Service, and Inventory data.
- **Phase 4 — US2 Creation/Approval**: Depends on Phase 3 master data and Phase 2 security.
- **Phase 5 — US3 Execution**: Depends on Phase 4 quotation approval and Phase 3 stock control.
- **Phase 6 — US4 Tracking**: Depends on persisted ServiceOrders from Phases 4–5.
- **Phase 7 — US5 Metrics**: Depends on lifecycle histories from Phase 5.
- **Phase 8 — API Quality**: Depends on all selected API stories.
- **Phase 9 — Quality Gates**: Depends on all selected implementation stories and API quality.
- **Phase 10 — Operations**: May begin after Phase 2 configuration; final validation depends on all stories.
- **Phase 11 — Documentation**: May begin with implemented concepts but final cross-check depends on code.
- **Phase 12 — Delivery**: Depends on all desired stories, quality gates, operations, and documentation.

### User Story Dependency Graph

```text
Setup -> Foundation -> US1 Records -> US2 Create/Approve -> US3 Execute -> US4 Track
                                                        \-> US5 Metrics
US1..US5 -> API Quality -> Quality Gates -> Delivery
Foundation -------------------------------> Operations --/
Implemented capabilities -----------------> Documentation /
```

### Within Each User Story

- Write and run the listed tests first; confirm they fail for the missing behavior.
- Implement domain/value models before persistence and application orchestration.
- Implement repository adapters before use cases that require them.
- Implement use cases before controllers.
- Complete integration and independent-test tasks before declaring the story done.
- Keep related tests with the behavior on the same capability branch whenever practical.

### Parallel Opportunities

- T004 and T005 can proceed together after build configuration.
- T008–T009 and T011 can proceed independently around the migration design.
- T013–T015 tests and T016–T021 shared types can be divided by file after their expected contracts agree.
- Within US1, Customer, Vehicle, Catalog, and Inventory domain/test groundwork can be developed on separate
  capability branches after Foundation, but Vehicle integration waits for Customer and later stories wait
  for all required master data.
- US2 domain quotation tests/models can proceed alongside persistence-port design before orchestration.
- US3 lifecycle, approval, and inventory tests can proceed in parallel before shared implementation files
  are changed.
- US4 tracking and administrative query tests can proceed in parallel.
- US5 unit and PostgreSQL query tests can proceed in parallel.
- Container, DDD, architecture, and security documentation branches can begin when their referenced behavior
  stabilizes, while their final validation waits for implementation.

---

## Parallel Example: User Story 1

```text
Branch feature/customer-management:
  T036 Customer domain tests -> T038 Customer aggregate
  T037 Customer API tests

Branch feature/service-catalog:
  T051 CatalogService domain tests -> T053 CatalogService aggregate
  T052 CatalogService API tests

Branch feature/inventory-management:
  T057 Inventory domain tests -> T059 Inventory aggregate
  T058 Inventory API tests

Branch feature/vehicle-management starts domain work in parallel, but ownership integration waits until
feature/customer-management is available in develop.
```

## Parallel Example: User Story 2

```text
T065 ServiceOrder creation tests
T066 Quotation calculation tests
T067 ServiceOrder component models
T068 Quotation model
T070 ServiceOrder application ports
```

## Parallel Example: User Story 3

```text
T081 Lifecycle transition tests
T082 Approval gate tests
T083 Additional repair tests
T084 Inventory concurrency integration tests
```

## Parallel Example: User Story 4

```text
T093 Tracking projection tests
T094 Customer token security tests
T095 Administrative query and query-count tests
T097 Customer tracking DTOs
T099 Administrative query use cases
```

## Parallel Example: User Story 5

```text
T102 Execution interval unit tests
T103 PostgreSQL metric query tests
```

---

## Implementation Strategy

### Foundation MVP

1. Complete Phase 1 and Phase 2.
2. Complete US1 to establish reliable master data.
3. Validate US1 independently before service-order work.

### Operational MVP

1. Add US2 to create immutable quotations and record customer decisions.
2. Add US3 to enforce execution, stock, additional approval, finish, and delivery.
3. Stop and demonstrate the complete internal repair-shop workflow.

### Incremental Completion

1. Add US4 restricted customer tracking and optimized administrative queries.
2. Add US5 lifecycle-derived execution metrics.
3. Complete API quality, coverage/performance gates, containers, documentation, security analysis, and
   release validation.

### Git Integration Strategy

1. Create cohesive branches from `develop` using the recommended names; do not create one branch per task.
2. Keep commits small, meaningful, green, and Conventional Commits-compliant.
3. Merge reviewed capability branches to `develop` only after their checkpoints pass.
4. Create `release/1.0.0` from `develop` only for final validation and delivery corrections.
5. Leave push, merge, release, and `v1.0.0` tag decisions to the developer unless explicitly delegated.

## Notes

- `[P]` means different files and no dependency on incomplete adjacent work; it does not override the phase
  dependency graph.
- Story labels provide traceability to [spec.md](spec.md).
- Each task or cohesive task group should leave the branch buildable and tests passing.
- Do not commit empty packages, duplicate Java/Kotlin components, real secrets, generated coverage output,
  or local `.env` files.
- Do not add microservices, brokers, Redis, CQRS infrastructure, event sourcing, Kubernetes, gateways,
  cloud resources, frontend/mobile clients, payments, or scheduling.
