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

The complete setup, architecture, API, security, Docker, testing, coverage, and delivery instructions will
continue to be expanded as their corresponding verified roadmap tasks are completed.
