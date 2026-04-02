# Draft: Auth Blueprint Quality Companion Baseline

목적: auth blueprint가 curated 승격 전 최소로 갖춰야 하는 테스트/verify/quality baseline을 고정한다.

## Test Baseline
### Backend integration
- signup/login/refresh/logout
- `/auth/me`
- RBAC enforcement
- email verify/resend/expiry
- provider-disabled path
- account-linking conflict path

### Frontend auth-state
- login success state
- unverified state handling
- refresh queue behavior
- explicit logout
- provider fallback UI

### Verify triplet
- security verify
- contract verify
- RBAC verify

### Key-flow E2E
- signup -> unverified -> verify -> login -> protected route
- provider login -> `/auth/me` -> role-aware route gate
- provider disabled -> fallback path

## Verify Expectations
- manifest-driven
- fail-open 금지
- golden case pass
- violation case reject
- false-positive 방지

## Quality Gates
- build
- lint (zero-warning goal)
- typecheck (zero-warning)
- tests
- verify (using `verify/manifest.schema.json` placeholder)
- reject simulation

## Evidence Expectations
- `.sisyphus/evidence/task-trio-*`
- curated-* evidence files
- ACTIVE-LOOP status와 교차 가능해야 함
- downstream curated evidence handoff는 `.sisyphus/drafts/auth-blueprint-curated-evidence-map.md`를 canonical curated evidence map으로 직접 참조해야 함
- Stage 3 baseline은 curated evidence map에 어떤 실행 증거가 아직 비어 있는지 넘겨줘야 하며, 그 map 없이 curated 주장 금지

## Explicit Exclusions
- broad real OAuth browser E2E
- profile/settings/admin feature tests
- non-auth feature tests
