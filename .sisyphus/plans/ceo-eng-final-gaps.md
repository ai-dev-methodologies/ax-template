# CEO/Eng Final Gaps — 누락 기능 전부 구현

## TL;DR

> CEO/Eng 리뷰에서 ACCEPTED 되었으나 미구현된 9개 항목을 7개 태스크로 정리하여 구현한다.
> 핵심: 프론트엔드 인터셉터, provider fallback, verify 자동 수정, OpenAPI→타입 동기화.
>
> **Estimated Effort**: Medium (1d)

---

## 미구현 항목 → 태스크 매핑

| 미구현 항목 | 태스크 |
|---|---|
| access token 만료 시 refresh 1회 → 재개 | T1 |
| refresh 실패 시 명시 로그아웃 유도 | T1 |
| 동시 401 → refresh mutex/queue | T1 |
| provider 장애 시 이메일 fallback | T2 |
| /auth/me 느림/실패 시 최소 UI 복구 | T3 |
| OpenAPI 변경 시 프론트 타입 일치 | T4 |
| PR/CI verify 실패 → 자동 수정/HITL | T5 |
| token 만료→refresh→재개 (Critical Path) | T1 (중복) |
| provider 장애→fallback (Critical Path) | T2 (중복) |

---

## TODOs

- [ ] 1. 프론트엔드 API 인터셉터 — 자동 refresh + mutex + 로그아웃

  **What to do**:
  - `frontend/src/lib/api/authClient.ts`에 응답 인터셉터 추가:
    - 401 응답 시 → refresh 토큰으로 자동 갱신 1회 시도
    - refresh 성공 → 원래 요청 재시도
    - refresh 실패 → authStore.logout() + `/login`으로 리다이렉트
  - `frontend/src/lib/auth/refresh-mutex.ts` 활용 — 동시 401 시 refresh 1회만 실행:
    - 첫 401 → refresh 진행
    - 이후 401 → 대기열에 넣고 refresh 완료 후 일괄 재시도
  - Vitest 테스트: 401 → refresh → 재시도 성공 / refresh 실패 → 로그아웃
  **Must NOT do**: backend 코드 변경 금지.

  **Commit**: `feat(frontend): API interceptor with auto-refresh, mutex, and forced logout`

- [ ] 2. Provider 장애 시 이메일 fallback

  **What to do**:
  - Backend: `OAuthService.handleCallback()`에서 provider API 호출 실패 시:
    - `ProviderUnavailableException` 발생
    - AuthExceptionHandler에서 catch → 503 + `{"message": "Provider unavailable", "fallback": "email"}`
  - Frontend: OAuth 로그인 실패 시 에러 메시지에 "이메일로 로그인" 링크 표시
  - 테스트: provider 장애 시뮬레이션 → 503 + fallback 응답 확인
  **Must NOT do**: 실제 provider 호출 (테스트에서 mock).

  **Commit**: `feat(auth): provider failure fallback to email login`

- [ ] 3. /auth/me 실패 시 최소 UI 복구

  **What to do**:
  - `frontend/src/lib/auth/authStore.ts`의 `fetchMe()`에 에러 처리 강화:
    - 타임아웃(5초) 설정
    - 실패 시 user를 null로 설정하되 accessToken 유지 → "프로필 로딩 실패" UI 표시
    - 재시도 버튼 제공
  - `frontend/src/pages/DashboardPage.tsx`에 에러 상태 UI:
    - loading 상태 → "로딩 중..."
    - error 상태 → "프로필을 불러올 수 없습니다. [재시도]"
  **Must NOT do**: backend 변경 금지.

  **Commit**: `feat(frontend): /auth/me failure graceful degradation with retry`

- [ ] 4. OpenAPI → 프론트엔드 타입 동기화 검증

  **What to do**:
  - `verify/check-types.sh` 스크립트 생성:
    - OpenAPI contract에서 request/response 스키마 추출
    - `frontend/src/lib/api/authClient.ts`의 TypeScript 인터페이스와 비교
    - 불일치 시 exit 1 + 불일치 필드 출력
  - `verify/checklist.yaml`에 `type-sync` check 추가
  - `verify/run-all.sh`에 `[6/6] Type Sync Check` 추가
  **Must NOT do**: 자동 코드 생성(openapi-generator 등) 도입 금지. 검증만.

  **Commit**: `feat(verify): OpenAPI-to-TypeScript type sync check`

- [ ] 5. Verify 실패 시 자동 재시도 + HITL 전환

  **What to do**:
  - `verify/run-with-retry.sh` 스크립트 생성:
    ```bash
    #!/bin/bash
    MAX_RETRIES=2
    for i in $(seq 1 $MAX_RETRIES); do
      echo "Attempt $i/$MAX_RETRIES..."
      if ./verify/run-all.sh; then
        echo "PASSED on attempt $i"
        exit 0
      fi
      echo "FAILED attempt $i — auto-fixing..."
      # 자동 수정 시도: format, lint fix 등
      cd backend && ./gradlew build -q 2>/dev/null; cd ..
    done
    echo "HITL REQUIRED: $MAX_RETRIES retries exhausted. Manual review needed."
    exit 1
    ```
  - `blueprints/auth-manifest.yaml` verification_stages에 retry 정책 추가
  **Must NOT do**: 복잡한 AI 자동 수정 로직 금지. 기본 빌드 재시도만.

  **Commit**: `feat(verify): auto-retry 2x then HITL escalation`

- [ ] 6. 프론트엔드 보호 라우트 가드

  **What to do**:
  - `frontend/src/components/ProtectedRoute.tsx` 생성:
    - 인증되지 않은 사용자 → `/login`으로 리다이렉트
    - 권한 부족 → "접근 권한이 없습니다" 표시
  - `frontend/src/App.tsx`에 `/dashboard`를 ProtectedRoute로 감싸기
  - Vitest 테스트: 미인증 → 리다이렉트 확인
  **Must NOT do**: 복잡한 role-based 라우팅 금지. 인증 여부만 확인.

  **Commit**: `feat(frontend): protected route guard with auth redirect`

- [ ] 7. 전체 자가검증 + ASVS 리포트 갱신

  **What to do**:
  - `verify/run-all.sh` 실행 → ALL PASSED 확인
  - `verify/run-checklist.sh` 실행 → 새 check 포함 전체 PASSED
  - Backend: `./gradlew testAsvs` → 26+ PASSED
  - Frontend: `npm run test` → 전체 PASSED
  - `specs/auth-asvs-l1-report.md` 갱신
  **Must NOT do**: 기존 기능 깨뜨리기 금지.

  **Commit**: `test: final verification after CEO/Eng gap fill`

---

## Execution Strategy

```
Wave 1 (병렬 — backend + frontend 독립):
├── T1: 프론트엔드 API 인터셉터 [visual-engineering]
├── T2: Provider 장애 fallback [deep]
└── T5: Verify 자동 재시도 + HITL [quick]

Wave 2 (W1 이후):
├── T3: /auth/me 실패 UI [visual-engineering]
├── T4: OpenAPI→타입 동기화 검증 [quick]
└── T6: 보호 라우트 가드 [visual-engineering]

Wave 3:
└── T7: 전체 자가검증 + 리포트 [quick]
```

---

## Success Criteria
```bash
verify/run-all.sh           # ALL PASSED
verify/run-checklist.sh     # ALL PASSED (새 check 포함)
cd backend && ./gradlew testAsvs  # 26+ ASVS PASS
cd frontend && npm run test       # ALL PASS
cd frontend && npm run build      # SUCCESS
```
