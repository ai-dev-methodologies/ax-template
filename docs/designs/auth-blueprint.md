# Design: ax-template Auth Blueprint

작성일: 2026-04-02
프로젝트: ax-template
상태: CANONICAL
기준 경로: `/Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template`
Supersedes: `docs/archive/2026-04-02-office-hours-design.md`
Related:
- `docs/plans/2026-04-02-auth-blueprint-ceo-plan.md`
- `docs/plans/2026-04-02-auth-blueprint-eng-review-test-plan.md`

## 1. 문제 정의
중소형 SI 회사는 AI를 조금씩 쓰고 있지만, 회사 차원의 통제 가능한 개발 시스템은 없다.
개발자들이 각자 AI를 활용해도 보안, 기능 정확성, 예측 가능성이 담보되지 않아 결국 기존 템플릿과 수동 검증으로 되돌아간다.
그 결과 AI는 생산성 도구가 아니라 추가 비용처럼 보이고, AX 전환이 빨라질수록 회사의 마진과 경쟁력이 흔들릴 위험이 커진다.

## 2. 제품 정의
`ax-template`의 V1은 일반적인 AI 코딩 도구가 아니다.

이 제품은 다음으로 정의한다.

> React + Spring Boot 기반 인증/보안 구현을, 회사 규율에 맞는 구조/계약/검증 루프로 강제하는 블루프린트 상품

즉 판매 단위는 단순 템플릿이 아니라 다음의 묶음이다.
- forkable template repo
- OpenAPI 계약 계층
- AI 규칙 팩
- verify 루프
- 테스트 기준
- 운영/롤백 기준

## 3. 타깃 사용자
### 1차 구매자
- 중소형 SI 회사 CEO
- 대략 20명 안팎의 개발자/디자이너/관리자를 운영하는 조직

### 구매 이유
- AI를 쓰더라도 사람 검증 비용이 너무 크면 마진이 개선되지 않는다
- 인증/보안은 반복되고 위험한 구간이라 표준화 가치가 높다
- 회사 고유 규율과 규칙을 자산으로 쌓아갈 수 있어야 한다

## 4. V1 범위
### 포함
- 새 프로젝트용 인증/보안 블루프린트
- React + Spring Boot 단일 repo
- OpenAPI/Swagger를 source of truth로 사용하는 계약 계층
- AI 작업 중 검증 + 로컬 verify + PR/CI 검증의 3단계 루프
- 단일 manifest 기반 규칙 정의와 verify 소비 구조
- 인증 제공자: 구글, 카카오, 이메일
- 인증 상태 방식: JWT access + refresh + HttpOnly cookie
- RBAC 3역할: admin / manager / member
- provider별 설정/프로퍼티 기반 feature flag
- provider 장애 시 provider별 실패 표시 + 이메일 fallback
- 미인증 상태 분리, 재발송, 만료 안내, idempotent 처리
- 명시적 account linking 흐름
- CSRF/CORS를 포함한 쿠키 기반 보안 설계
- refresh token 상태 저장
- refresh rotation의 grace window + 명시 로그
- 모킹 중심 검증 전략
- 핵심 경로만 E2E

### 제외
- 브라운필드 적용 레이어
- 고객사별 override / company pack layer
- generator / CLI 제품화
- 실 OAuth 브라우저 E2E 우선 전략
- 넓은 E2E 범위

## 5. 핵심 설계 원칙
1. **Boring by default**
   - Spring Security 내장 JWT 흐름을 우선 사용한다
   - provider flag는 외부 플랫폼이 아니라 설정/프로퍼티 기반으로 간다
2. **Single source of truth**
   - rules와 verify는 따로 관리하지 않는다
   - 단일 manifest를 기준으로 AI 규칙과 verify 기준을 같이 끌고 간다
3. **Explicit over clever**
   - 역할이 보이는 boring 네이밍을 쓴다
   - 인증 흐름과 실패 상태를 숨기지 않는다
4. **Strong verification**
   - plan → validate → execute
   - 실패 시 자동 수정 2회 후 HITL
   - 보안/계약/RBAC 위반은 즉시 실패

## 6. 아키텍처 방향
### Repo 구조
```text
ax-template/
├── frontend/
├── backend/
├── contracts/
├── blueprints/
├── verify/
└── docs/
```

### 시스템 흐름
```text
요구사항
  ↓
AI 작업 계획 생성
  ↓
중간 검증 (manifest / contract / rules)
  ↓
코드 생성 또는 수정
  ↓
로컬 verify
  ↓
PR/CI verify
  ↓
사람 승인 또는 예외 처리
```

### 인증 흐름 기준
```text
Provider Login / Email Signup
          ↓
Identity Resolution
          ↓
Explicit Account Linking (if needed)
          ↓
Access Token 발급
Refresh Token 저장
          ↓
/auth/me 로 최소 UI 상태 조회
          ↓
보호 라우트 접근
```

## 7. 계약 계층
- OpenAPI/Swagger는 문서가 아니라 **source of truth**다
- 프론트 타입/클라이언트는 계약 계층과 일치해야 한다
- 백엔드 구현이 계약을 어기면 verify에서 실패해야 한다

## 8. 검증 루프
### 3단계 검증
1. AI 작업 중 검증
2. 로컬 verify
3. PR/CI verify

### verify 강제 기준
- 보안 위반
- 계약 위반
- RBAC 위반

### 실패 복구
- 자동 수정 2회
- 이후 HITL

## 9. 테스트 전략
### 4축 테스트 세트
1. Spring 통합 테스트
2. React 인증 상태 테스트
3. verify 스크립트 테스트
4. 핵심 로그인 E2E

### verify 스크립트 테스트 3종
- golden case
- violation case
- false-positive 방지

### E2E 범위
- 로그인
- 로그아웃
- 보호 라우트 접근/차단
- 미인증 상태 차단

실 OAuth 브라우저 흐름 전체 자동화는 V1에서 제외한다.
대신 provider mock 기반 검증을 우선한다.

## 10. 보안 기준
- env + 시크릿 매니저 호환
- HttpOnly cookie 기반 인증 상태
- CSRF/CORS 필수 포함
- refresh token 상태 저장
- refresh rotation grace window + 명시 로그
- 미인증 상태는 별도 상태로 분리
- provider 실패 시 fallback 경로 보장

## 11. 성능/안정성 기준
- 프론트 refresh mutex/queue로 동시 401 폭주 방지
- `/auth/me`는 최소 UI 상태만 반환
- provider별 feature flag로 부분 롤백 가능
- 전체 롤백은 백업 전략으로 유지

## 12. KPI
V1의 핵심 지표는 아래 3개다.
- 검증 통과율
- 재작업률
- 인증 구현 리드타임

이 3개가 좋아지지 않으면, 이 블루프린트는 제품 가치가 약한 것이다.

## 13. 구현 순서 제안
1. contracts 뼈대 정의
2. backend 인증 체인 최소 수직 슬라이스
3. frontend 최소 auth state + 보호 라우트
4. verify 루프와 manifest 연결
5. 테스트 4축 최소 세트
6. provider flag 및 롤백 기준 정리

## 14. 다음 단계
이 문서는 구현 직전의 canonical design이다.
다음 행동은 아래 중 하나다.
- 이 문서를 기준으로 구현 계획서 작성
- 실제 `ax-template` repo 안에 폴더/manifest/contract skeleton 생성
- auth UX에 대한 별도 design review 수행
