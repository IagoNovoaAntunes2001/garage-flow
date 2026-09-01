# Phase 1 Submission Checklist

- [x] Backend APIs complete for Phase 1 MVP
- [x] Customer CRUD
- [x] Vehicle CRUD
- [x] Service CRUD
- [x] Parts/Supplies CRUD
- [x] Inventory control
- [x] ServiceOrder creation
- [x] Quotation
- [x] Customer approval
- [x] Customer tracking
- [x] Execution-time metric
- [x] JWT administrative authentication
- [x] CPF/CNPJ validation
- [x] License plate validation
- [x] Unit tests
- [x] Integration tests
- [x] Swagger/OpenAPI
- [x] Dockerfile
- [x] docker-compose.yml
- [x] README
- [x] DDD documentation
- [x] ServiceOrder Event Storming
- [x] Parts/Supplies Event Storming
- [x] DDD diagrams
- [x] Ubiquitous Language
- [x] Vulnerability analysis report template
- [x] Vulnerability scan executed with final local tool output
- [ ] Group name:
- [ ] Participant names:
- [ ] Discord usernames:
- [ ] Documentation link:
- [ ] Private repository link:
- [ ] Demonstration video:

Repository note: the repository must remain private and provide access to `soat-architecture`.

## Validation Evidence

Date: 2026-08-31.

- `./gradlew --no-daemon clean test`: PASS, 78 tests.
- `./gradlew --no-daemon build`: PASS, including `koverVerify`.
- `./gradlew --no-daemon clean check koverVerify koverHtmlReport`: PASS, run 1 of 3.
- `./gradlew --no-daemon clean check koverVerify koverHtmlReport`: PASS, run 2 of 3.
- `./gradlew --no-daemon clean check koverVerify koverHtmlReport`: PASS, run 3 of 3.
- Kover HTML report: `build/reports/kover/html/index.html`.
- `docker compose config`: PASS.
- `docker compose build backend`: PASS.
- `docker compose up -d postgres backend`: PASS.
- `docker compose ps`: backend healthy; PostgreSQL healthy.
- Backend logs confirmed Flyway applied V1 from an empty schema and Hibernate validation completed.
- `docker compose down`: PASS.

Security scan note: Trivy was executed through Docker. The final filesystem scan has no findings. The final
image scan has 0 HIGH and 0 CRITICAL findings; remaining LOW/MEDIUM findings are documented in
`docs/security/vulnerability-report.md`.

## Release Validation Evidence

Date: 2026-09-01.

- Branch: `release/1.0.0`.
- Task audit: `specs/001-repair-shop-mvp/tasks.md` has T001 through T146 marked complete and no pending `[ ]` task entries.
- Placeholder audit: no empty `*Test.kt` placeholders or unfinished-work markers were found in production/test source, docs, README, or Spec Kit task file.
- `GRADLE_USER_HOME=/private/tmp/tech-challenge-gradle ./gradlew --no-daemon clean test`: PASS, 89 tests.
- `GRADLE_USER_HOME=/private/tmp/tech-challenge-gradle ./gradlew --no-daemon build`: PASS, including `koverVerify`.
- `GRADLE_USER_HOME=/private/tmp/tech-challenge-gradle ./gradlew --no-daemon koverVerify`: PASS.
- `git diff --check`: PASS.
- `docker compose down -v`: PASS; project PostgreSQL volume removed before clean migration validation.
- `docker compose build --no-cache`: PASS; final backend image built as `tech-challenge-backend` with image id `sha256:59535b893bf191a625a0e566ba54c67f37c03a6133427f3f24224cb0de7819c5`.
- `docker compose up -d postgres backend`: PASS; PostgreSQL and backend reached healthy status.
- Flyway empty-database validation: PASS; V1 `initial schema` applied successfully from an empty PostgreSQL volume.
- PostgreSQL schema inspection: PASS; 14 public tables, 37 indexes, 30 check constraints, 13 foreign keys, 14 primary keys, 6 unique constraints, and one successful `flyway_schema_history` entry.
- Hibernate startup validation: PASS; backend started successfully against the Flyway-created schema.
- Swagger/OpenAPI validation: PASS; `/v3/api-docs` was available and documented admin security while leaving customer tracking public.
- Authentication validation: PASS; administrator token issued by `/api/v1/auth/token`; unauthenticated admin request rejected with 401.
- API smoke validation: PASS; exercised Customer, Vehicle, Service Catalog, Parts/Supplies, inventory stock adjustment, ServiceOrder creation, quotation request, customer approval, execution, finish, delivery, invalid transition rejection, customer tracking, and execution metrics.
- Smoke evidence: `SMOKE_OK customer=e85e7869-cb0e-4989-a186-117b2f2d5896 vehicle=7284cf83-4015-45cf-bcf5-950563d6a38d service=1e310b4d-491b-4831-bea4-5016db1176a8 part=6ccb97ad-354f-4959-ba4f-d4e752df88f5 supply=16edba68-1a1b-48b1-b1fa-5ad5ba52bb06 order=6a35fe91-7136-4760-84fd-16741eeb4e64 part_quantity=4 metric_count=1`.
- Sensitive-data exposure validation: PASS; smoke test checked authentication error response and recent backend logs for passwords, Bearer/JWT tokens, and full CPF values.
- Persistence restart validation: PASS; after `docker compose down` and `docker compose up -d postgres backend` without deleting the PostgreSQL volume, the delivered ServiceOrder and consumed inventory quantity remained available.
- Trivy filesystem scan: PASS; no Dockerfile misconfigurations or secrets detected.
- Trivy image scan: PASS for release gate; 0 HIGH and 0 CRITICAL findings. Remaining LOW/MEDIUM findings are documented in `docs/security/vulnerability-report.md`.

Release review corrections made before opening the PR:

- Fixed execution-time metrics to calculate using full status history for ServiceOrders finished inside the requested period.
- Stabilized PostgreSQL integration tests by limiting test Hikari pools and cleaning business tables between integration tests.
- Replaced previously empty critical test placeholders with executable focused tests.
- Hardened Spring Security fallback from `permitAll` to `denyAll` for unspecified routes.
- Added explicit OpenAPI Bearer security annotations to administrative operations while preserving public customer approval/tracking endpoints.
- Optimized ServiceOrder administrative list mapping with batch loading for items, quotations, quotation lines, approvals, and status history.
- Added standardized 404 API response handling for unmapped resources.
- Sanitized request logging to avoid exposing CPF/CNPJ-like values or token-shaped values in logged paths.
