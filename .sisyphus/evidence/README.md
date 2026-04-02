# Evidence Map

이 디렉토리는 `auth-blueprint-foundations-trio`와 이후 curated 승격 과정에서 생성되는 검증 증거를 저장한다.

## Foundations Trio Evidence Targets
- `task-trio-1-toolchain.txt` — pinned versions 핵심 키 존재 검증
- `task-trio-1-scope.txt` — pinned versions에 auth scope 밖 항목 미포함 검증
- `task-trio-2-manifest-fields.txt` — auth-manifest policy field completeness 검증
- `task-trio-2-machine-readable.txt` — auth-manifest YAML parse 및 key presence 검증
- `task-trio-3-endpoints.txt` — auth-openapi endpoint boundary audit
- `task-trio-3-edge-states.txt` — auth-openapi edge state/error coverage audit
- `task-trio-4-verify-schema.txt` — verify placeholder required field set 검증

## Curated Promotion Evidence Targets
- `curated-build.txt`
- `curated-lint.txt`
- `curated-type.txt`
- `curated-test.txt`
- `curated-verify.txt`
- `curated-reject-simulation.txt`
- `curated-fail-open-audit.txt`

## Rule
증거가 없으면 승격 주장은 invalid다.
