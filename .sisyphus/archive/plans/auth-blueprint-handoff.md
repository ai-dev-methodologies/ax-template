# Auth Blueprint Handoff

## Status
READY_FOR_EXECUTION

## Primary execution plans
- `.sisyphus/plans/auth-blueprint-execution.md`
- `.sisyphus/plans/auth-blueprint-stage3-assets.md`
- `.sisyphus/plans/auth-blueprint-foundations-trio.md`

## Canonical project docs
- `docs/designs/auth-blueprint.md`
- `docs/plans/auth-blueprint-implementation-plan.md`
- `docs/TEMPLATE-GOVERNANCE.md`
- `docs/governance/TEMPLATE-LIFECYCLE.md`
- `docs/governance/ACTIVE-LOOP-TEMPLATE.md`
- `docs/plans/auth-blueprint-candidate-inventory.md`
- `docs/plans/auth-blueprint-reference-selection.md`
- `docs/plans/auth-blueprint-promotion-checklist.md`
- `docs/governance/upstream-watchlist.yaml`
- `ACTIVE-LOOP.md`

## What is already locked
- New-project-only auth/security blueprint
- React + Spring Boot single repo
- OpenAPI schema-first source of truth
- Spring Security built-in JWT flow
- Stateful refresh token
- JWT access/refresh + HttpOnly cookie
- CSRF/CORS mandatory
- Explicit account linking
- Providers: Google + Kakao + email
- RBAC: admin / manager / member
- Provider flags via config/properties
- Verify fails on security/contract/RBAC violations
- AI/local/PR verify loop
- Tests: Spring integration + React auth-state + verify + key-flow E2E
- Real OAuth browser E2E excluded for V1

## Remaining small decisions to close at kickoff
1. Exact patch pins for Java / Spring Boot / Spring Security / Node / React / Orval
2. Exact request/response shape for `POST /auth/link-account`
3. Final HTTP semantics for refresh race / grace window edge case
4. Exact backend build plugin and OpenAPI generation plugin versions

## Recommended default posture for kickoff
- Prefer boring stable patch lines over latest
- Keep `/auth/me` minimal, do not expand into profile/settings
- Keep OpenAPI static even when provider is disabled
- Keep account linking explicit, do not auto-merge by email

## Immediate execution order
1. Finalize `blueprints/pinned-versions.yaml`
2. Finalize `contracts/auth-openapi.yaml`
3. Finalize `blueprints/auth-manifest.yaml` and `blueprints/auth-checklist.md`
4. Create backend/frontend/verify skeletons
5. Wire tests and curated-promotion evidence

## Success condition for first execution slice
The project should move from policy-heavy draft state to a real Stage 3 canonical draft with enough evidence to attempt `draft -> curated` promotion.
