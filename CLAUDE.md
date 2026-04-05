# ax-template — Auth Blueprint Template

## Project Identity

이 프로젝트는 React + Spring Boot 앱이 아니라 **contract-first, manifest-driven, verify-driven auth blueprint**이다.
핵심은 구현 코드가 아니라 **spec 파일 3종**이다:

| File | Role |
|------|------|
| `specs/auth-asvs-l1.yaml` | OWASP ASVS L1 검증 항목 (26개). @Tag 테스트와 1:1 대응 |
| `contracts/auth-openapi.yaml` | API 계약. 모든 엔드포인트의 source of truth |
| `blueprints/auth-manifest.yaml` | 정책값 (JWT, 세션, rate limit, CORS, provider 설정) |

구현 코드(`backend/`, `frontend/`)는 이 spec 파일들의 **참조 구현**이다.

## Core Methodology

### 검증 피드백 루프 (Validated)
```
규칙 정의 (YAML spec) → TDD 구현 → @Tag 테스트로 기계적 검증
     ↑                                        ↓
     └────── 실패 시 수정 후 재검증 ──────────┘
```

- `./gradlew testAsvs` — 단일 명령으로 ASVS 전체 pass/fail 판정
- VIOLATION 테스트로 검증: 규칙 위반 시 테스트가 잡아냄 (증명됨)

### 외부 참조 정규화
외부 best practice(OWASP, Spring Docs, Vercel React 등)를 그대로 사용하지 않는다.
반드시 **내부 canonical 구조로 흡수**:
- OWASP ASVS → `specs/auth-asvs-l1.yaml` 항목으로 pinned
- Spring Security 패턴 → `blueprints/auth-manifest.yaml` 정책으로 정규화
- API 규약 → `contracts/auth-openapi.yaml` 스키마로 계약화

### Portable Test Template
`specs/portable-test-template/`에 RestAssured 기반 이식 가능한 테스트 예시 제공.
새 프로젝트에서 spec 파일 3개 + 테스트 템플릿을 복사하면 검증 피드백 루프 즉시 사용 가능.

## Anti-Patterns (금지)

### 거버넌스 무한루프 ❌
과거 실패: 30+ 문서와 18 세션을 소비했지만 코드 0줄.
원인: draft→curated→stable 승격 게이트가 데드락을 만듦.

**금지 사항**:
- `TEMPLATE-GOVERNANCE.md` 같은 승격 절차 문서 생성 금지
- "curated promotion check" 같은 게이트 프로세스 금지
- "evidence bundle" 같은 검증-위한-검증 문서 금지
- 구현 없이 문서만 생산하는 계획 수립 금지

**대신**: 코드를 먼저 쓰고, `./gradlew testAsvs`로 검증하고, 부족하면 spec을 보강한다.

### MockMvc 전용 테스트 ❌
MockMvc 테스트는 프로젝트 패키지 구조에 결합되어 이식 불가.
ASVS 검증에는 RestAssured (black-box HTTP) 사용 권장.

## Build & Test

```bash
# Backend
cd backend && ./gradlew build      # 빌드
cd backend && ./gradlew test       # 전체 테스트
cd backend && ./gradlew testAsvs   # ASVS 검증만 실행

# Frontend
cd frontend && npm run build       # 빌드
cd frontend && npm run test        # 테스트
```

## Architecture

```
ax-template/
├── specs/                     # ASVS 검증 스펙 (핵심)
│   ├── auth-asvs-l1.yaml
│   ├── auth-asvs-l1-report.md
│   └── portable-test-template/
├── contracts/                 # OpenAPI 계약 (핵심)
│   └── auth-openapi.yaml
├── blueprints/                # 정책 매니페스트 (핵심)
│   └── auth-manifest.yaml
├── backend/                   # Spring Boot 참조 구현
├── frontend/                  # React 참조 구현
└── docs/archive/              # 과거 거버넌스 문서 (참고용)
```

## Auth Coverage

| 방식 | 엔드포인트 | ASVS 항목 |
|------|-----------|----------|
| Email | signup, login, verify, refresh, logout, /me, password-reset/change, resend-verification | 23개 (V2.1.x, V2.2.1, V2.5.x, V2.7.x, V3.x, V4.x) |
| OAuth | authorize, callback, link, unlink (Google/Naver/Kakao) | 3개 (V2.8.1, V2.8.2, V2.8.3) |
| **Total** | **14 endpoints** | **26 ASVS items, 26 COVERED** |

## Methodology

이 프로젝트의 방법론은 `METHODOLOGY.md`에 문서화되어 있다.
핵심: **Spec Trio** (Compliance Spec + API Contract + Policy Manifest) → TDD 구현 → 단일 명령 검증 (`./gradlew test{Domain}`).

새 도메인 템플릿을 만들 때 `METHODOLOGY.md`의 5단계 + Dry-Run Checklist를 따른다.
