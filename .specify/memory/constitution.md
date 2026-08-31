<!--
Sync Impact Report
- Version change: 1.0.0 -> 2.0.0
- Modified principles:
  - I. Maintainable, Explicit Code -> III. Code Quality and Maintainability
  - II. Tests Define Confidence -> IV. Testing Standards
  - III. Consistent User Experience -> VI. Consistent REST APIs
  - IV. Measured Performance -> VIII. Observability and Performance
- Added principles:
  - I. Domain-Driven Design First
  - II. Layered Modular Monolith
  - V. Security by Design
  - VII. Service Order Integrity
  - IX. Reproducible Development Environment
  - X. MVP Scope Discipline
- Added sections:
  - Architecture and API Constraints
  - Delivery and Quality Gates
- Removed sections:
  - Engineering Standards (requirements incorporated into the new principles and constraints)
  - Delivery and Review Gates (replaced by Delivery and Quality Gates)
- Follow-up TODOs: none
-->
# Automotive Repair Shop Backend Constitution

## Core Principles

### I. Domain-Driven Design First
Business rules MUST be represented explicitly in the domain model. The ubiquitous language of the
repair shop MUST be used consistently in code, documentation, APIs, tests, and team communication.
Customers, vehicles, service orders, services, inventory, quotations, and approvals MUST have clearly
separated domain responsibilities and explicit relationships. Business invariants and state changes
MUST be enforced by domain objects or domain services, not only by controllers or request validation.
The domain model MUST NOT depend directly on databases, web frameworks, messaging, or other
infrastructure concerns. This preserves the business model as the authoritative source of behavior.

### II. Layered Modular Monolith
The application MUST be delivered as a modular monolith with distinct domain, application/use-case,
infrastructure, and API/interface layers. Dependencies MUST point inward toward domain rules: API and
infrastructure MAY depend on application contracts, and application code MAY depend on the domain, but
the reverse dependencies MUST NOT occur. Module boundaries MUST reflect business capabilities and MUST
communicate through explicit contracts rather than another module's internal implementation. The MVP
MUST NOT introduce microservices or distributed-system infrastructure without an approved requirement
that cannot be satisfied by the monolith. This architecture minimizes operational cost while preserving
clear paths for future evolution.

### III. Code Quality and Maintainability
Code MUST favor simple, explicit solutions over clever or speculative abstractions. SOLID principles
MUST be applied when they improve cohesion, substitutability, or change isolation; they MUST NOT be used
to justify unnecessary indirection. Business rules MUST have one authoritative implementation.
Methods, classes, modules, and use cases MUST have focused responsibilities and meaningful,
domain-oriented names. Public contracts and non-obvious decisions MUST be documented. Compiler errors,
relevant warnings, static-analysis failures, and known critical defects MUST be resolved before merge.
These constraints keep behavior understandable and changes safe.

### IV. Testing Standards
Automated tests are mandatory for critical business flows. Each critical domain module MUST maintain at
least 80% automated line coverage, measured by the project's standard coverage task; coverage MUST NOT
replace assertions about behavior. Unit tests MUST validate domain invariants, calculations, rejection
paths, and state transitions. Integration tests MUST validate persistence, REST contracts,
authentication and authorization, and the principal service-order workflows. Every defect correction
MUST add a regression test that fails without the fix. Tests MUST be deterministic, independent,
reproducible, and executable through the Gradle wrapper. The complete required test suite MUST pass
before merge.

### V. Security by Design
Every administrative API MUST require valid JWT authentication and explicit authorization appropriate
to the operation. Authentication and authorization adapters MUST remain separate from core domain
logic. Inputs, including CPF, CNPJ, and vehicle license plates, MUST be validated for format and domain
constraints before use; invalid data MUST be rejected predictably. APIs, logs, errors, and telemetry
MUST NOT expose credentials, secrets, password hashes, tokens, personal data beyond the intended
contract, or internal implementation details. Secrets MUST be supplied through external configuration
and MUST NOT be committed. Application code, container images, and dependencies MUST support automated
vulnerability scanning, and unresolved critical vulnerabilities MUST block release.

### VI. Consistent REST APIs
APIs MUST use consistent RESTful resource naming, HTTP methods, status codes, pagination where needed,
request validation, and a shared error-response format. API contracts MUST use the repair shop's
ubiquitous language and remain consistent across modules. Every public endpoint MUST be documented with
OpenAPI/Swagger or an equivalent machine-readable specification, including authentication, inputs,
responses, validation failures, and relevant examples. Customer-facing service-order tracking MUST
expose only information needed by that customer and MUST NOT reveal administrative, security-sensitive,
or unrelated customer data. Breaking contract changes MUST have an explicit versioning and migration
plan.

### VII. Service Order Integrity
Service order status transitions MUST follow an explicit domain workflow, and invalid transitions MUST
be rejected without partial changes. Quotations MUST be calculated from registered services, parts,
supplies, quantities, and applicable prices; their calculation rules MUST be auditable and tested.
Additional repairs MUST require recorded customer approval whenever the business workflow requires it,
and work MUST NOT advance past that approval gate without authorization. Inventory reservations,
consumption, returns, and adjustments MUST preserve stock consistency and prevent impossible balances.
Operations that modify a service order and related inventory or approval state MUST be transactional
where partial completion would violate an invariant.

### VIII. Observability and Performance
Request paths MUST avoid N+1 access patterns, unnecessary database queries, unbounded result sets, and
avoidable repeated work. Each critical API or use case MUST define a measurable response-time and
resource budget during planning, using representative data and medium-sized repair-shop load; a change
MUST meet that budget before release. Performance claims MUST be supported by reproducible measurement,
and regressions exceeding an approved budget MUST block release unless explicitly accepted with a
time-bounded remediation plan. Important failures, service-order transitions, approvals, inventory
changes, and quotation operations MUST produce useful structured logs with correlation context and no
sensitive data. The system MUST record the timestamps necessary to calculate and report average service
execution time using a documented start and completion definition.

### IX. Reproducible Development Environment
The complete application and its required local dependencies MUST run through Docker. The repository
MUST provide a maintained Dockerfile and Docker Compose configuration with deterministic build and
startup behavior. README.md MUST document prerequisites, configuration, build, test, database setup,
startup, health verification, and shutdown commands. Secrets or machine-specific paths MUST NOT be
embedded in container definitions. A new developer MUST be able to clone the repository and start the
system by following only the documented steps; those steps MUST be verified whenever environment or
dependency configuration changes.

### X. MVP Scope Discipline
Implementation MUST prioritize only capabilities required by the Tech Challenge and its accepted
feature specifications. Microservices, event brokers, frontend applications, cloud infrastructure, and
other operational components MUST NOT be introduced unless a concrete approved requirement demonstrates
their necessity. Extension points MUST be based on an identified variation or boundary, not a predicted
future need. Proposals that increase architectural or operational complexity MUST document the current
problem, simpler alternatives considered, and measurable benefit. This protects delivery speed without
preventing later evolution through clean domain and module boundaries.

## Architecture and API Constraints

- Java 17 and the Gradle wrapper MUST remain the canonical compilation and build environment unless a
  constitution amendment changes the platform.
- Domain modules MUST expose explicit application-facing contracts; controllers and persistence
  implementations MUST remain adapters around those contracts.
- Database schemas and transactions MUST protect identifiers, relationships, stock consistency, and
  service-order workflow invariants in addition to domain-level validation where appropriate.
- Administrative and customer-facing endpoints MUST have distinguishable authorization policies.
- A shared API convention MUST define error shape, validation details, timestamps, identifiers,
  pagination, and status-code mapping before multiple resource APIs are implemented.
- Logs and metrics MUST enable diagnosis of important workflows and measurement of service execution
  time without becoming an alternative source of business truth.

## Delivery and Quality Gates

Every feature specification MUST identify affected domain concepts, business invariants, API contracts,
security and privacy implications, required tests, and measurable performance expectations. Plans MUST
show layer and module ownership and MUST justify any new dependency or infrastructure component.

Before merge, the author MUST demonstrate that the Gradle build, automated tests, coverage threshold,
API contract checks, and applicable security scans pass. Changes to critical workflows MUST include unit
and integration evidence. Changes to the development environment MUST include a successful clean Docker
startup using the documented README steps. Reviewers MUST reject dependency-direction violations,
duplicated business rules, inconsistent API behavior, security exposure, invalid service-order or stock
transitions, unexplained performance regressions, and unjustified scope expansion.

An exception to a MUST rule requires a written rationale, risk assessment, accountable owner, expiration
date, and maintainer approval. An exception MUST NOT silently weaken domain integrity, authentication,
authorization, sensitive-data protection, or transactional consistency.

## Governance

This constitution is the highest-priority engineering policy for the project. Specifications, plans,
tasks, implementation, reviews, and releases MUST demonstrate compliance. Amendments MUST be proposed in
writing with their motivation, affected principles, compatibility impact, and migration plan when
applicable. A maintainer MUST approve the amendment before it takes effect.

Versions follow semantic versioning: MAJOR for incompatible governance changes, principle removals, or
material redefinitions; MINOR for new principles or materially expanded obligations; and PATCH for
clarifications that do not change obligations. Every amendment MUST update the Sync Impact Report,
version, and Last Amended date. Reviewers MUST perform a constitution compliance check on every feature
and pull request. Non-compliance MUST be corrected before merge or recorded through the explicit,
time-bounded exception process defined above.

**Version**: 2.0.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-08-31
