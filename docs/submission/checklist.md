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
