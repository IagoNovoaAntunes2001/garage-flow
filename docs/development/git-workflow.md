# Version Control and Implementation Workflow

Garage Flow uses a lightweight Git Flow model coordinated with the Spec Kit task roadmap. This document
records the workflow used during Phase 1 implementation so contributors can reproduce it consistently.

## Permanent branches

- `main` contains stable, deliverable Tech Challenge versions.
- `develop` is the integration branch for ongoing work.

Regular implementation is never committed directly to either permanent branch. GitHub should protect both
branches against force pushes, require pull requests, and require successful CI checks when CI is available.
At least one approval is preferred for group work.

## Implementation cycle

Each roadmap increment follows the same controlled sequence:

1. Confirm the previous pull request was reviewed and merged by a developer.
2. Run `git status` and `git branch --show-current`; stop if unexpected local changes exist.
3. Check out `develop` and run `git pull origin develop`.
4. Confirm the previous task range is present and marked complete in `tasks.md`.
5. Create the next capability branch from the updated `develop`.
6. Read the Spec Kit constitution, specification, plan, tasks, research, data model, OpenAPI contract, and
   quickstart as sources of truth.
7. Implement only the task range assigned to that branch. Behavioral tasks follow red-green-refactor: write
   the test, observe the expected failure, implement the behavior, and observe it passing.
8. Run focused tests, the broader suite where practical, and `./gradlew build`.
9. Mark only verified tasks complete in `specs/001-repair-shop-mvp/tasks.md`.
10. Create one or more meaningful Conventional Commits, push the implementation branch, and open a pull
    request against `develop`.
11. Stop. The implementation coordinator never merges its own pull request and does not begin the next
    branch until a developer confirms the merge.

The pull request body records the completed task range, implementation summary, test and build evidence,
technical decisions or deviations, and unresolved issues.

## Branch naming and roadmap

Tasks are grouped into cohesive capabilities rather than one branch per task:

| Order | Branch | Spec Kit tasks |
|---:|---|---|
| 1 | `chore/kotlin-migration` | T001–T006 |
| 2 | `chore/project-foundation` | T007–T026 |
| 3 | `feature/authentication` | T027–T035 |
| 4 | `feature/customer-management` | T036–T043 |
| 5 | `feature/vehicle-management` | T044–T050 |
| 6 | `feature/service-catalog` | T051–T056 |
| 7 | `feature/inventory-management` | T057–T064 |
| 8 | `feature/service-order` | T065–T073 |
| 9 | `feature/quotation-approval` | T074–T080 |
| 10 | `feature/service-order-execution` | T081–T092 |
| 11 | `feature/customer-tracking` | T093–T101 |
| 12 | `feature/execution-metrics` | T102–T108 |
| 13 | `feature/api-quality` | T109–T113 |
| 14 | `test/integration-suite` | T114–T119 |
| 15 | `chore/docker-setup` | T120–T125 |
| 16 | `docs/ddd-documentation` | T126–T130 |
| 17 | `docs/architecture` | T131–T136 |
| 18 | `docs/security-analysis` | T137–T140 |
| 19 | `release/1.0.0` | T141–T146 and final validation |

Supported prefixes are `feature/`, `fix/`, `refactor/`, `docs/`, `chore/`, `test/`, `release/`, and
`hotfix/`. Feature and maintenance branches originate from `develop`; urgent released-version corrections
originate from `main` and return to both permanent branches through reviewed pull requests.

## Commits

Commits use Conventional Commits with `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, or `ci`.
Scopes normally name a business capability, for example:

```text
feat(customer): implement customer management
test(service-order): cover lifecycle transitions
docs(ddd): document ubiquitous language
chore(database): configure PostgreSQL and Flyway
```

A commit represents one meaningful change and is not artificially divided by file. Related tests normally
travel with the implemented behavior. Intentionally broken code is never committed.

## Release and hotfix flow

When Phase 1 is deliverable, `release/1.0.0` originates from `develop`. Only final fixes, documentation,
Docker and Swagger checks, complete tests, coverage, vulnerability analysis, and submission preparation
belong there. Its reviewed pull request targets `main`; equivalent release changes must also remain in
`develop`. Tag `v1.0.0` means “Phase 1 Tech Challenge” and is created only after explicit developer approval.

Hotfixes originate from `main` as `hotfix/<description>` and return through reviewed pull requests to both
`main` and `develop`.

## Automation safety

The implementation coordinator may inspect Git, create branches, modify project files, run validation,
commit, push implementation branches, and open pull requests. It does not merge pull requests, force push,
discard user changes, delete remote branches, create tags, or create releases without explicit instruction.

The following commands are prohibited in the normal workflow:

```text
git reset --hard
git clean -fd
git push --force
git push --force-with-lease
```
