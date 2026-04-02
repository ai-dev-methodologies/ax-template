# Draft: Auth Blueprint Scaffold Asset Set

목적: `auth-blueprint` V1에서 실제로 필요한 최소 파일군을 디렉토리별로 고정한다.

## frontend/
- `package.json`
- `src/app/` 또는 router entry
- `src/features/auth/`
  - login
  - signup
  - verify-email-result
  - protected-route-guard
- `src/lib/auth/`
  - auth-state model
  - refresh queue/mutex helper
- `src/lib/api/`
  - generated client entry
  - auth client wrapper
- `tests/`
  - auth-state tests
  - key-flow UI tests

## backend/
- `build.gradle.kts`
- `src/main/java/.../auth/`
  - auth controller
  - auth service
  - auth config/security config
- `src/main/java/.../security/`
  - JWT / resource server / csrf-cors baseline
- `src/main/java/.../user/`
  - user identity / provider link / refresh token model boundary
- `src/main/resources/application.yml`
- `src/test/java/.../`
  - auth integration tests
  - security tests

## contracts/
- `auth-openapi.yaml`

## blueprints/
- `pinned-versions.yaml`
- `auth-manifest.yaml`
- `auth-checklist.md`

## verify/
- `manifest.schema.json`
- `scripts/`
- `fixtures/`
- `README.md`

## .github/workflows/
- auth blueprint CI baseline workflow (lint/type/test/verify skeleton)

## Out of scope in V1 scaffold
- profile/settings pages
- role administration UI/API
- brownfield migration helpers
- multi-service split repo support
- generator/CLI wrapper
