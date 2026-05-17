# backend-reference (FROZEN — moved from `backend/` on 2026-05-17)

Spring Boot 3.5 + Java 21 reference workload. Demonstrates ax-template's
methodology applied end-to-end to a real auth + CRUD + rate-limit codebase
with ASVS L1 compliance.

## Status

**FROZEN v1.0.** Kept as the worked example. No new features, no domain
additions. Bug fixes accepted if regression breaks `./gradlew test{Domain}`
PASS.

## Run

From this directory (formerly the repo's `backend/`):

```bash
cd archive/backend-reference
./gradlew test           # full
./gradlew testAsvs       # auth ASVS 26 items
./gradlew testCrud       # CRUD spec
./gradlew testPractices  # 64 practices rules
./gradlew testPortability  # advisory: rules applied to external fixtures
```

`testPractices` runs against the **frozen** `practices/` catalog — see
`../../practices/STATUS.md`.

## What's here

| Path | Purpose |
|------|---------|
| `src/main/java/com/ax/template/authblueprint/` | auth + RBAC + OAuth + rate-limit Spring Security |
| `src/main/java/.../crud/` | CRUD reference (5 endpoints, 7 security tests) |
| `src/test/java/.../` | TDD test suite — `@Tag("ASVS")`, `@Tag("CRUD")`, `@Tag("PRACTICES")`, `@Tag("PORTABILITY")` |
| `build.gradle.kts` | Gradle KTS with separate test tasks per domain |

## Domains

| 도메인 | Spec | Endpoints | Items | Test task |
|---|---|---|---|---|
| Auth | `../../specs/auth-asvs-l1.yaml` | 14 | 26 ASVS | `testAsvs` |
| CRUD | `../../specs/crud-l0.yaml` | 5 | 7 security tests | `testCrud` |
| Rate-limit | `../../specs/ratelimit-l0.yaml` | 1 (`/api/ratelimit/ping`) | 4 items | `testRatelimit` |
| Practices | `../../specs/spring-practices-l0.yaml` | n/a | 64 rules | `testPractices` |

## Why moved

Round 3 strategic review (2026-05-17) — see top-level `CLAUDE.md`. ax-template's
active wedge is now the React ESLint plugin in `practices-react/`, not Spring
Boot. The Spring Boot workload remains a valid reference but no longer
front-and-center.

## Path migration

Old → new path mapping:

```
backend/                              → archive/backend-reference/
backend/src/main/java/com/ax/...      → archive/backend-reference/src/main/java/com/ax/...
backend/build.gradle.kts              → archive/backend-reference/build.gradle.kts
cd backend && ./gradlew testX         → cd archive/backend-reference && ./gradlew testX
```

If you have local notes / scripts that reference the old path, update accordingly.
