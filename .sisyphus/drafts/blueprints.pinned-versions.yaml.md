# Draft File: blueprints/pinned-versions.yaml

```yaml
java:
  version: "21"
  policy: lts
  rationale: "Spring Boot 3.x와 enterprise Java 표준에 맞는 LTS"

spring_boot:
  version: "3.2.x"
  policy: stable-train
  rationale: "Spring Security 6.2.x와 정렬되고 ecosystem 호환성이 좋음"

spring_security:
  version: "6.2.x"
  policy: stable-train
  rationale: "built-in JWT resource server 흐름의 boring default"

node:
  version: "20.x"
  policy: active-lts
  rationale: "React/Vite/tooling 호환성과 CI 안정성"

react:
  version: "19.x"
  policy: stable-major
  rationale: "current stable ecosystem target, experimental feature 비의존"

openapi:
  spec_version: "3.0.3"
  rationale: "generator compatibility, boring default"

frontend_codegen:
  tool: "orval"
  version: "6.x"
  rationale: "OpenAPI source-of-truth에서 typed client generation을 가장 직접적으로 지원"

frontend_test:
  unit_runner: "vitest"
  component_test: "@testing-library/react"
  e2e: "playwright"
  rationale: "auth state / UI / key-flow E2E baseline"

backend_test:
  integration: "Spring Boot Test"
  http: "MockMvc or WebTestClient"
  container_db: "Testcontainers"
  rationale: "security and auth integration baseline"

build:
  backend: "gradle-kotlin"
  frontend: "vite"
  rationale: "boring default, docs/examples availability, CI ergonomics"

constraints:
  disallow:
    - pre-release
    - alpha
    - beta
    - rc
  if_latest_conflicts_with_stability: choose_stable_enough
```

## Notes
- exact patch pin은 구현 kickoff에서 마지막으로 고정
- UI library는 여기 넣지 않음
