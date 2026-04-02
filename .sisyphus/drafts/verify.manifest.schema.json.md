# Draft File: verify/manifest.schema.json

## Purpose
Wave 1에서는 완전한 JSON Schema가 목적이 아니다.
목적은 아래 두 가지다.
1. manifest 필수 필드 누락을 기계적으로 잡기
2. verify engine이 manifest를 읽는다는 사실을 구조로 고정하기

## Required field set
- template_id
- family
- status
- stack
- provider_policy
- auth_state
- rbac
- account_linking
- verification_policy
- rate_limits
- security_defaults
- testing_baseline
- verification_checkpoints
- must_not
- reject_if
- source_precedence
