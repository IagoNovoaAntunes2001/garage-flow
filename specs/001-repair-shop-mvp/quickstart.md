# Quickstart Validation Guide: Automotive Repair Shop MVP

This guide defines the end-to-end checks the implementation must support. Commands describe the intended
finished project; they are not expected to work until their implementation tasks are complete.

## Prerequisites

- Docker Engine with Docker Compose
- Java 17 for host-side Gradle commands
- `curl` and a JSON processor such as `jq`
- Trivy for the delivery security scan

Copy `.env.example` to `.env` and replace development placeholders with local-only values. At minimum,
configure PostgreSQL database/user/password, a JWT signing secret of at least 256 bits, and initial
administrator username/password. Never commit `.env`.

## 1. Verify the Kotlin Migration and Build

```bash
./gradlew clean check koverVerify koverHtmlReport
```

Expected outcomes:

- The build uses Java 17 and compiles production/test Kotlin.
- No production Java implementation remains under `src/main/java`.
- Unit and integration tests pass independently of execution order.
- Kover enforces at least 80% line coverage for critical domain packages.
- The HTML coverage report is generated under `build/reports/kover/`.

## 2. Start a Clean Local Environment

```bash
docker compose build
docker compose up -d
docker compose ps
```

Expected outcomes:

- PostgreSQL becomes healthy before the backend reports readiness.
- Flyway applies every migration to the empty database.
- Hibernate validates rather than creates or updates the schema.
- The backend health endpoint reports healthy without exposing sensitive details.
- Repeating startup does not reapply successful migrations or lose persisted data.

Inspect backend logs and confirm that database passwords, administrator passwords, JWTs, tracking tokens,
CPF/CNPJ values, and stack traces are absent.

## 3. Inspect API Documentation

Open the local Swagger UI URL documented in README.md and retrieve the generated OpenAPI document.
Compare its paths, security scheme, request/response semantics, status codes, and error model with
[contracts/openapi.yaml](contracts/openapi.yaml). Swagger UI must allow a locally issued Bearer token and
must not be publicly enabled by the production profile.

## 4. Authenticate an Administrator

Request a token through `POST /api/v1/auth/token` with the bootstrap administrator credentials.

Expected outcomes:

- Valid credentials return a short-lived Bearer access token.
- Invalid credentials return the standard 401 error without identifying which credential was wrong.
- Calling any `/api/v1/admin/**` operation without the token returns 401.
- Calling with a valid token lacking required authority returns 403.

Keep the access token only in a local shell variable and never paste it into committed files.

## 5. Create Master Data

Using the administrative API:

1. Create one CPF customer and one CNPJ customer.
2. Find each by a formatted and normalized document value.
3. Attach a legacy-plate vehicle and a Mercosur-plate vehicle to the correct customers.
4. Create at least two catalog services.
5. Create one Part and one Supply with positive stock.
6. List and update every resource through paginated endpoints.

Expected outcomes:

- Invalid check digits, repeated CPF/CNPJ digits, invalid plates/years, duplicate normalized identifiers,
  negative prices, and negative stock are rejected.
- A vehicle cannot be used for a customer who does not own it.
- Updating current prices does not rewrite any existing service-order quotation snapshot.
- Removing referenced records deactivates them while preserving historical reads.

## 6. Execute the Principal Service-Order Flow

Follow the contract to:

1. Create an order by customer document and owned vehicle with at least one Service plus Part/Supply lines.
2. Save the one-time customer tracking token returned at creation.
3. Start diagnosis.
4. Generate the quotation and request approval.
5. Read the restricted customer tracking view with the order ID and token.
6. Approve the current quotation version with the customer token.
7. Start execution and verify required stock is consumed once.
8. Finish the order and deliver the vehicle.

Expected outcomes:

- The observed statuses are exactly RECEIVED, IN_DIAGNOSIS, AWAITING_APPROVAL, IN_EXECUTION, FINISHED,
  and DELIVERED in valid order.
- Each transition has an immutable timestamped history entry.
- Quotation service subtotal plus inventory subtotal equals total using captured unit prices.
- Execution before approval and every unlisted transition fail without partial state or stock changes.
- Repeating execution does not consume stock twice.
- Complete administrative details include history; customer tracking excludes documents, internal notes,
  stock, credentials, and administrative data.

## 7. Validate Additional Repairs

Create and approve another order, start execution, add an additional service or inventory item, and request
new approval.

Expected outcomes:

- The prior quotation remains immutable and becomes superseded.
- The new version contains the additional scope and exact new total.
- The order returns to AWAITING_APPROVAL and cannot resume until that exact version is approved.
- Rejecting the new version does not authorize work.
- Approval permits resumption and consumes only outstanding added inventory.
- A competing request that would over-consume stock loses cleanly and stock never becomes negative.

## 8. Validate Tracking Security

Try a valid order ID with no token, a random token, another order's token, an expired/revoked token, and the
valid token.

Expected outcomes:

- All invalid combinations disclose no order details and return an indistinguishable not-found response.
- The valid token exposes only the restricted order identity, status, progress, current customer-visible
  quotation state, and last relevant update.
- Customer access never grants an administrative operation.

## 9. Validate Execution Metrics

Create lifecycle histories with one continuous execution interval and one order that pauses for additional
approval. Request `/api/v1/admin/metrics/execution-time` with an inclusive `from` and exclusive `to` range.

Expected outcomes:

- The average equals the independently calculated per-order sum of active IN_EXECUTION intervals.
- Time in AWAITING_APPROVAL is excluded.
- The response reports the eligible order count.
- A period with no eligible orders returns count zero and no average rather than a fabricated zero duration.

## 10. Validate Performance and Query Behavior

Load representative data up to the planning baseline and exercise primary reads with 50 simultaneous users.

Expected outcomes:

- At least 95% of valid primary requests finish within 2 seconds.
- Collection responses are bounded to size 1..100.
- List queries do not load complete quotation/history graphs.
- Query observation confirms no N+1 pattern for customer vehicles, order summaries, or order details.

Record the data volume, concurrency, machine/container resources, commands, and measured percentiles so the
result is reproducible.

## 11. Run Vulnerability Analysis

Build the final image, then run the Trivy filesystem and image scans documented in
`docs/security/vulnerability-report.md`.

Expected outcomes:

- The report records scan date, commands, targets, findings, severities, analysis, and remediation.
- Findings are not suppressed merely to make the report clean.
- No unresolved critical vulnerability remains at delivery.

## 12. Verify Documentation and Delivery

Confirm README.md links every required document and matches the actual package names, commands, ports,
configuration, lifecycle, security flow, and terminology. Render every Mermaid diagram and compare it with
implemented aggregates and transitions. Complete `docs/submission/checklist.md` without inventing group,
participant, Discord, repository, documentation, or video values. Confirm it states that the repository
must remain private and grant access to `soat-architecture`.

## 13. Stop the Environment

```bash
docker compose down
```

Use the documented explicit volume-removal command only when intentionally validating a fresh migration;
normal shutdown must preserve local database data.
