# Draft: Auth Blueprint Curated Evidence Map

목적: Stage 3 산출물이 Stage 4 curated promotion 체크의 어떤 항목을 닫는지 대응표로 고정한다.

| Stage 3 asset | Closes which curated gate? | Evidence file / source |
|---|---|---|
| `blueprints/pinned-versions.yaml` | toolchain truth, pre-release 금지, stable-enough rationale | `task-trio-1-toolchain.txt`, `task-trio-1-scope.txt` |
| `contracts/auth-openapi.yaml` | static contract, auth endpoint boundary, edge-state/error coverage | `task-trio-3-endpoints.txt`, `task-trio-3-edge-states.txt` |
| `blueprints/auth-manifest.yaml` | policy source, rate limit/provider/token/RBAC/reject rules | `task-trio-2-manifest-fields.txt`, `task-trio-2-machine-readable.txt` |
| `blueprints/auth-checklist.md` | visible pass/fail review checks | file review + checklist audit |
| `verify/manifest.schema.json` | verify consumer 필수 field set 고정 | `task-trio-4-verify-schema.txt` |
| scaffold asset set draft | 실제 Stage 3 asset 범위 고정 | draft review |
| architecture baseline draft | backend/frontend/verify 경계 규칙 고정 | draft review |
| quality companion draft | 테스트/verify/reject simulation baseline 고정 | draft review |
| `.sisyphus/evidence/README.md` | evidence path convention 고정 | README itself |

## Curated Gate still requiring execution evidence
이 항목들은 draft만으로는 안 닫힌다.
- build 기준 확인
- lint 기준 확인
- type 기준 확인
- test 기준 확인
- verify 기준 확인
- reject simulation 통과
- fail-open 항목 0 확인

## Meaning
즉 Stage 3 draft asset이 존재한다고 curated가 되는 건 아니다.
Stage 3은 curated 진입 자격을 만들고,
Stage 4는 실제 검증 evidence로 curated를 주장할 수 있게 만든다.

## Detailed Checklist Mapping against `docs/plans/auth-blueprint-promotion-checklist.md` and `ACTIVE-LOOP.md`
- `auth-blueprint-promotion-checklist.md` references require:
  - `official_doc_refs`, `approved_github_refs`, and `practical refs` to be explicitly linked.
  - `must_not`, `reject_if`, and `anti-pattern` to be captured in the quality companion and manifest rules.
  - `testing baseline` and `verify checkpoints` to be fully established and automated.
  - `chub` freshness checks to validate API documentation is up-to-date.
- `ACTIVE-LOOP.md` requires:
  - Explicit `ACTIVE-LOOP evidence` demonstrating actual execution in a verifiable environment, not just drafting.
