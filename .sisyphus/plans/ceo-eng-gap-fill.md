# CEO/Eng Review Gap Fill — 월요일 발표 준비

## TL;DR

> CEO/Eng 리뷰에서 ACCEPTED 되었으나 구현되지 않은 항목을 모두 메꾼다.
> 스크립트 기반 기계적 검증 체계, RBAC, provider feature flag, forkable template 가이드, KPI를 추가한다.
>
> **Estimated Effort**: Large (1-2d)
> **Deadline**: 월요일 발표

---

## Context

### CEO 리뷰 원본: `docs/archive/plans/2026-04-02-auth-blueprint-ceo-plan.md`
### Eng 리뷰 원본: `docs/archive/plans/2026-04-02-auth-blueprint-eng-review-test-plan.md`
### Office Hours: `docs/archive/2026-04-02-office-hours-design.md`

---

## TODOs

- [x] 1. 3단계 검증 루프 — 스크립트 기반 강제 실행 체계

  **What to do**:
  - `verify/` 디렉토리에 실행 가능한 검증 스크립트 체계 구축
  - **Stage 1: AI 작업 중 검증** — `verify/check-contract.sh`: OpenAPI 계약 준수 확인
    - 구현된 controller endpoint가 OpenAPI path와 1:1 대응하는지 검사
    - 응답 스키마가 계약과 일치하는지 검사
    - 위반 시 즉시 exit 1
  - **Stage 2: 로컬 verify** — `verify/run-all.sh`: 전체 검증 한 줄 실행
    - `./gradlew testAsvs` (ASVS compliance)
    - `verify/check-contract.sh` (계약 준수)
    - `verify/check-security.sh` (보안 규칙: 하드코딩된 secret 검사, CSRF, cookie flags)
    - `verify/check-rbac.sh` (RBAC 규칙 검사)
    - 하나라도 실패 → exit 1 + 위반 항목 출력
  - **Stage 3: PR/CI 검증** — `verify/ci-gate.sh`: CI에서 실행하는 최종 게이트
    - `verify/run-all.sh` + frontend build + lint
    - PR merge 조건으로 사용
  - `blueprints/auth-manifest.yaml`에 `verification_stages` 섹션 추가
  - `METHODOLOGY.md`에 3단계 검증 루프 반영
  **Must NOT do**: 실제 CI 파이프라인 구축 금지. 스크립트만.

  **Acceptance Criteria**:
  - [ ] `verify/run-all.sh` 실행 → exit 0 (모든 검증 통과)
  - [ ] `verify/check-contract.sh` 실행 → 계약 위반 감지
  - [ ] `verify/check-security.sh` 실행 → 보안 위반 감지
  - [ ] 의도적 위반 시 → exit 1 + 위반 메시지

  **Commit**: `feat(verify): 3-stage verification loop with script-based enforcement`

- [x] 2. RBAC 3역할 — admin/manager/member

  **What to do**:
  - `UserEntity`에 role 필드 확인 (이미 존재할 수 있음)
  - `UserRole` enum: ADMIN, MANAGER, MEMBER (현재 값 확인 후 맞춤)
  - SecurityConfig에 역할 기반 접근 제어:
    - `/api/admin/**` → ADMIN만
    - `/api/manage/**` → ADMIN + MANAGER
    - `/api/**` → 인증된 사용자
  - 최소 보호 라우트 1개: `GET /api/admin/users` (admin만 접근)
  - ASVS spec에 RBAC 검증 항목 추가
  - RBAC 테스트: member가 admin 엔드포인트 접근 → 403
  **Must NOT do**: 복잡한 permission 시스템 금지. 역할 3개 + 경로 기반만.

  **Acceptance Criteria**:
  - [ ] admin 전용 엔드포인트 존재
  - [ ] member → admin 엔드포인트 → 403
  - [ ] admin → admin 엔드포인트 → 200

  **Commit**: `feat(auth): RBAC admin/manager/member with protected routes`

- [x] 3. 스크립트 기반 TODO 검증 체계

  **What to do**:
  - `verify/checklist.yaml` — 기계적 검증 가능한 TODO 목록
  ```yaml
  checks:
    - id: "contract-match"
      description: "OpenAPI 계약과 구현 엔드포인트 1:1 대응"
      script: "verify/check-contract.sh"
      type: "automated"
    - id: "asvs-compliance"
      description: "ASVS spec의 모든 applicable 항목에 @Tag 테스트 존재"
      script: "./gradlew testAsvs"
      type: "automated"
    - id: "no-hardcoded-secrets"
      description: "소스 코드에 하드코딩된 비밀번호/API키 없음"
      script: "verify/check-security.sh"
      type: "automated"
    - id: "rbac-enforced"
      description: "역할 기반 접근 제어가 동작함"
      script: "verify/check-rbac.sh"
      type: "automated"
  ```
  - `verify/run-checklist.sh` — checklist.yaml을 순회하며 각 스크립트 실행, 결과 리포트 생성
  **Must NOT do**: 수동 검토가 필요한 항목 포함 금지. 모든 항목이 스크립트로 자동 실행.

  **Acceptance Criteria**:
  - [ ] `verify/run-checklist.sh` → 모든 check PASS, 리포트 출력
  - [ ] checklist.yaml에 4개 이상 자동화된 check 존재

  **Commit**: `feat(verify): script-based TODO checklist with automated execution`

- [x] 4. Provider feature flag

  **What to do**:
  - `blueprints/auth-manifest.yaml`에 provider별 enabled/disabled 플래그:
  ```yaml
  provider_flags:
    email: true
    google: true
    naver: true
    kakao: true
  ```
  - `application.yml`에 feature flag 설정 반영
  - OAuthController에서 disabled provider 접근 시 → 403 + structured error
  - 테스트: disabled provider로 authorize 요청 → 403

  **Acceptance Criteria**:
  - [ ] provider를 disabled로 설정 → authorize 403
  - [ ] enabled provider → 정상 동작

  **Commit**: `feat(auth): provider-level feature flags`

- [x] 5. Forkable template 가이드

  **What to do**:
  - `docs/GETTING-STARTED.md` 생성:
    - 1. Fork/clone this repo
    - 2. 환경변수 설정 (OAuth client-id/secret)
    - 3. `cd backend && ./gradlew build`
    - 4. `cd frontend && npm install && npm run build`
    - 5. `verify/run-all.sh` 실행하여 전체 검증
    - 6. 커스터마이즈: manifest 정책값 수정, provider flag 조정
  - `README.md` 업데이트 또는 생성

  **Acceptance Criteria**:
  - [ ] `docs/GETTING-STARTED.md` 존재
  - [ ] 새 개발자가 따라할 수 있는 5단계 가이드

  **Commit**: `docs: GETTING-STARTED.md — forkable template guide`

- [x] 6. KPI 측정 기준

  **What to do**:
  - `blueprints/auth-manifest.yaml`에 `kpi` 섹션 추가:
  ```yaml
  kpi:
    verification_pass_rate:
      description: "verify/run-all.sh 통과율"
      target: "100%"
      measurement: "verify/run-all.sh exit code"
    rework_rate:
      description: "검증 실패 후 재작업 비율"
      target: "<10%"
      measurement: "git log에서 fix: 커밋 비율"
    implementation_lead_time:
      description: "spec 정의부터 testAsvs PASS까지 소요 시간"
      target: "<2d per endpoint group"
      measurement: "git log 타임스탬프 diff"
  ```
  - `verify/report-kpi.sh` — 현재 KPI 수치 출력 스크립트

  **Acceptance Criteria**:
  - [ ] manifest에 kpi 섹션 존재
  - [ ] `verify/report-kpi.sh` 실행 → KPI 수치 출력

  **Commit**: `feat: KPI measurement criteria in manifest + report script`

- [x] 7. METHODOLOGY.md + CLAUDE.md 최종 보강

  **What to do**:
  - METHODOLOGY.md에 반영:
    - 3단계 검증 루프 (Step 5 확장)
    - 스크립트 기반 checklist 패턴
    - RBAC 패턴 (구조적 패턴으로, auth-specific이 아님)
    - KPI 측정 패턴
  - CLAUDE.md에 반영:
    - verify/ 스크립트 사용법
    - RBAC 커버리지
    - KPI 참조

  **Acceptance Criteria**:
  - [ ] METHODOLOGY.md에 검증 루프 3단계 설명 존재
  - [ ] CLAUDE.md에 verify/ 참조 존재

  **Commit**: `docs: final reinforcement of METHODOLOGY.md and CLAUDE.md`

---

## Execution Strategy

```
Wave 1 (병렬):
├── Task 1: 3단계 검증 루프 스크립트 [unspecified-high]
├── Task 2: RBAC 3역할 [deep]
└── Task 4: Provider feature flag [quick]

Wave 2 (T1 이후):
├── Task 3: 스크립트 기반 TODO 체크리스트 [unspecified-high]
└── Task 5: Forkable template 가이드 [quick]

Wave 3:
├── Task 6: KPI [quick]
└── Task 7: METHODOLOGY + CLAUDE.md 보강 [quick]
```

---

## Success Criteria
```bash
verify/run-all.sh           # Expected: exit 0, 모든 검증 통과
verify/run-checklist.sh     # Expected: 4+ checks PASS
verify/report-kpi.sh        # Expected: KPI 수치 출력
cd backend && ./gradlew testAsvs  # Expected: 26+ ASVS tests pass
```
