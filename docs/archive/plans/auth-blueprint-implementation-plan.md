# Plan: ax-template Auth Blueprint Implementation

작성일: 2026-04-02
프로젝트: ax-template
상태: READY_FOR_IMPLEMENTATION
기준 문서:
- `docs/designs/auth-blueprint.md`
- `docs/plans/2026-04-02-auth-blueprint-ceo-plan.md`
- `docs/plans/2026-04-02-auth-blueprint-eng-review-test-plan.md`

## 1. 목표
`ax-template` V1은 React + Spring Boot 기반의 **새 프로젝트용 인증/보안 블루프린트**를 제공한다.

이 계획의 구현 목표는 다음 세 가지를 한 번에 충족하는 것이다.
1. 실제로 동작하는 인증 블루프린트
2. OpenAPI 기반 계약 계층
3. 규칙을 강제하는 verify 루프

즉, 단순 샘플 코드가 아니라 **구조 + 계약 + 검증**이 함께 있는 최소 수직 슬라이스를 만든다.

## 2. 이번 구현의 범위
### 포함
- 단일 repo 기반의 React + Spring Boot 구조
- 인증 제공자: 구글, 카카오, 이메일
- JWT access + refresh + HttpOnly cookie
- 상태 저장 refresh token
- OpenAPI source of truth
- `/auth/me` 최소 UI 상태 조회
- RBAC 3역할: admin / manager / member
- 미인증 상태 분리
- 이메일 인증 재발송 / 만료 안내 / idempotent 처리
- 명시적 account linking 흐름
- provider별 설정/프로퍼티 기반 flag
- CSRF/CORS 포함
- AI 작업 중 검증 + 로컬 verify + PR/CI verify
- 단일 manifest 기반 rules + verify 소비 구조
- 4축 테스트 세트

### 제외
- 브라운필드 적용
- 고객사 override layer
- generator / CLI
- 실 OAuth 브라우저 E2E 전면 적용
- 넓은 범위의 E2E 확대

## 3. 설계 고정값
이 값들은 구현 중 다시 흔들지 않는다.

- 백엔드 인증 체인: **Spring Security 내장 JWT 흐름**
- refresh token: **상태 저장**
- provider flag: **설정/프로퍼티 기반**
- OpenAPI: **source of truth**
- verify 기준: **보안 / 계약 / RBAC 위반 즉시 실패**
- 테스트 전략: **unit / integration / E2E 계층 분리**
- E2E 범위: **핵심 경로만**
- 프론트 refresh 처리: **mutex/queue**
- `/auth/me`: **최소 UI 상태만 반환**
- account linking: **명시적 흐름**
- CSRF/CORS: **V1 필수**
- refresh rotation: **grace window + 명시 로그**

## 4. 제안 디렉토리 구조
```text
ax-template/
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   ├── features/auth/
│   │   ├── lib/api/
│   │   └── lib/auth/
│   ├── tests/
│   └── package.json
├── backend/
│   ├── src/main/java/.../
│   ├── src/main/resources/
│   ├── src/test/java/.../
│   ├── build.gradle.kts or pom.xml
│   └── ...
├── contracts/
│   └── auth-openapi.yaml
├── blueprints/
│   ├── auth-manifest.yaml
│   └── auth-checklist.md
├── verify/
│   ├── scripts/
│   ├── fixtures/
│   └── README.md
├── docs/
│   ├── designs/
│   ├── plans/
│   └── archive/
└── .github/
    └── workflows/
```

## 5. 핵심 흐름 다이어그램
### 5.1 시스템 구성
```text
                +----------------------+
                |      contracts/      |
                |   auth-openapi.yaml  |
                +----------+-----------+
                           |
             source of truth for API/auth contract
                           |
         +-----------------+-----------------+
         |                                   |
         v                                   v
+------------------+                +------------------+
|     backend/     |                |     frontend/    |
| Spring Security  |                | auth state / UI  |
| JWT + refresh    |                | typed client      |
+---------+--------+                +---------+--------+
          |                                   |
          +-----------------+-----------------+
                            |
                            v
                    +---------------+
                    |   blueprints/  |
                    | manifest/rules |
                    +-------+-------+
                            |
                            v
                     +-------------+
                     |   verify/   |
                     | local + CI  |
                     +-------------+
```

### 5.2 인증 흐름
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
/auth/me 조회
          ↓
보호 라우트 접근
```

### 5.3 검증 흐름
```text
작업 요청
  ↓
AI 계획 생성
  ↓
manifest / contract 기준 중간 검증
  ↓
코드 생성 또는 수정
  ↓
로컬 verify
  ↓
성공 → PR/CI verify → 승인
실패 → 자동 수정 2회 → 실패 지속 시 HITL
```

## 6. 구현 단계
## Phase 0. 저장소 부트스트랩
### 목적
빈 `ax-template`를 실제 구현 가능한 repo 형태로 만든다.

### 산출물
- 기본 디렉토리 구조 생성
- frontend/backend/contracts/blueprints/verify skeleton 생성
- 문서 경로 정리

### 완료 기준
- 루트에서 구조가 한 번에 보인다
- 각 디렉토리에 최소 README 또는 placeholder가 있다

---
## Phase 1. 계약 계층 선행 정의
### 목적
백엔드/프론트보다 먼저 인증 계약을 고정한다.

### 파일 후보
- `contracts/auth-openapi.yaml`

### 포함할 엔드포인트 초안
- `POST /auth/email/signup`
- `POST /auth/email/login`
- `POST /auth/email/resend-verification`
- `GET /auth/verify-email`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/link-account`
- provider 시작/콜백 경로 정의용 문서 섹션

### 완료 기준
- request/response shape 고정
- auth 상태 응답 최소 필드 정의
- role field와 미인증 상태 shape 정의
- 프론트 타입/클라이언트 생성 기준 확정

### 리스크
- Spring Security filter 흐름과 OpenAPI의 긴장 관계
- provider callback 경로 모델링 방식 애매함

---
## Phase 2. 백엔드 최소 수직 슬라이스
### 목적
Spring Boot 쪽 인증 중심축을 만든다.

### 핵심 구성
- Spring Security 내장 JWT 리소스 서버 흐름
- access/refresh token 발급
- refresh token 상태 저장 모델
- provider별 설정/프로퍼티 flag
- RBAC 3역할
- CSRF/CORS 기본 정책
- `/auth/me`
- 미인증 사용자 상태
- account linking 기본 흐름

### 파일/모듈 후보
- `backend/src/main/java/.../auth/`
- `backend/src/main/java/.../security/`
- `backend/src/main/java/.../user/`
- `backend/src/main/resources/application-*.yml`

### 완료 기준
- 이메일 기반 signup/login/verify/resend 동작
- 구글/카카오 provider flag 구조 존재
- refresh token 저장/회전/로그 모델 존재
- `/auth/me`가 최소 UI 상태를 반환
- provider failure가 이메일 fallback 경로와 연결

### 주의
- custom filter 남발 금지
- refresh token 완전 stateless 금지
- SameSite/CSRF/CORS 공백 금지

---
## Phase 3. 프론트 최소 인증 UX 슬라이스
### 목적
React 쪽에서 실제 사용자 흐름을 최소 범위로 연결한다.

### 화면/상태
- 로그인 화면
- 회원가입 화면
- 이메일 인증 결과 화면
- 보호 라우트 차단 상태
- 미인증 사용자 상태 안내

### 핵심 로직
- `/auth/me` 기반 최소 auth state
- refresh mutex/queue
- provider 버튼 표시/숨김은 provider flag 기준
- provider 실패 시 provider별 메시지 + 이메일 fallback 안내

### 파일/모듈 후보
- `frontend/src/features/auth/`
- `frontend/src/lib/auth/`
- `frontend/src/lib/api/`
- `frontend/src/app/`

### 완료 기준
- 로그인/로그아웃 흐름 동작
- 미인증 상태에서 보호 기능 차단
- token 만료 시 refresh 1회 후 원 요청 재개
- refresh 실패 시 명시 로그아웃

---
## Phase 4. Blueprint / Verify 계층 연결
### 목적
문서가 아니라 실제 강제 메커니즘을 만든다.

### 핵심 산출물
- `blueprints/auth-manifest.yaml`
- `blueprints/auth-checklist.md`
- `verify/scripts/*`
- `verify/fixtures/*`

### 원칙
- manifest가 진실 공급원
- rules와 verify는 manifest를 소비
- 수동 이중 정의 금지

### verify 실패 기준
- 보안 위반
- 계약 위반
- RBAC 위반

### 완료 기준
- golden case 통과
- violation case 차단
- false-positive 방지
- 로컬에서 동일 기준 재현 가능

---
## Phase 5. 테스트 4축 구축
### 1) Spring 통합 테스트
- signup/login/refresh/logout
- `/auth/me`
- RBAC 접근 제어
- email verify/resend/expiry
- provider mock 연동
- account linking 흐름

### 2) React 인증 상태 테스트
- 로그인 후 상태 반영
- 미인증 상태 처리
- refresh queue 동작
- provider flag에 따른 버튼/흐름 변경

### 3) verify 스크립트 테스트
- golden case
- violation case
- false-positive 방지

### 4) 핵심 E2E
- 이메일 가입 → 미인증 → 인증 메일 확인 → 로그인 → 보호 라우트 접근
- 구글 로그인 → `/auth/me` → 역할 기반 화면 표시
- 카카오 로그인 → linking 필요 시 linking → 재접근
- provider 장애 → fallback 경로 진입

### 완료 기준
- 4축 모두 존재
- verify 자체도 테스트됨
- 실 OAuth 브라우저 전체 흐름은 아직 제외

---
## Phase 6. 운영/배포 최소 기준
### 목적
첫 배포에서 망가졌을 때 바로 복구할 수 있게 한다.

### 포함
- provider별 설정/프로퍼티 flag
- provider 우선 롤백
- 전체 롤백 백업
- 핵심 KPI 수집 포인트 정의

### KPI
- 검증 통과율
- 재작업률
- 인증 구현 리드타임

### 완료 기준
- provider별 on/off 가능
- 부분 실패 시 부분 롤백 가능
- KPI가 어디서 측정되는지 문서화됨

## 7. 최소 구현 순서
```text
1. contracts/
2. backend auth core
3. frontend auth UI/state
4. blueprints + verify
5. tests
6. rollout / metrics
```

이 순서를 바꾸지 않는 이유는,
계약이 먼저 없으면 프론트와 백엔드가 서로 다른 방향으로 가고,
verify가 너무 늦게 들어오면 규칙이 구현 뒤에 붙는 장식이 되기 때문이다.

## 8. Acceptance Criteria
- `ax-template`가 단일 repo 구조로 부트스트랩된다
- OpenAPI 계약이 source of truth로 존재한다
- Spring Boot 최소 인증 체인이 동작한다
- React 최소 auth UX가 동작한다
- 구글/카카오/이메일 범위가 문서와 코드에서 일치한다
- 미인증 상태, 재발송, 만료 안내, idempotent 처리 존재
- refresh token 상태 저장과 grace window 정책 존재
- verify 루프가 로컬/PR에서 실제로 실패를 발생시킨다
- 테스트 4축이 모두 존재한다
- provider별 flag와 롤백 기준이 존재한다

## 9. What already exists
재사용 대상:
- `docs/reference/BP-TEMPLATE-PACK-SCHEMA.md`
- `docs/reference/BP-TEMPLATE-PACK-CONSUMER-CONTRACT.md`
- `scripts/bp_pack_consumer.py`
- `tests/test_bp_pack_consumer.py`
- `bp-development-guide.md`
- `bp-e2e-service`의 산출물 패턴

새로 만들어야 하는 것:
- Spring Boot 실제 블루프린트 자산
- React 실제 auth UX 자산
- `ax-template` 내부 manifest / contract / verify 연결

## 10. NOT in scope
- 브라운필드 적용
- 고객사 override layer
- generator / CLI
- 실 OAuth 브라우저 E2E 우선 적용
- 넓은 E2E 범위

## 11. 다음 단계
이 계획서 다음 행동은 하나다.

> `ax-template` 안에 Phase 0~1 skeleton을 실제로 만들고, 그 위에서 backend auth core를 시작한다.

즉, 다음 구현 세션은 **빈 repo를 실제 구조로 부트스트랩하는 작업**부터 시작하면 된다.
