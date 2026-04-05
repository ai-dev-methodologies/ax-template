# Getting Started — ax-template

## 1. Fork or Clone
```bash
git clone https://github.com/your-org/ax-template.git my-project
cd my-project
```

## 2. 환경변수 설정
```bash
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export NAVER_CLIENT_ID=your-naver-client-id
export NAVER_CLIENT_SECRET=your-naver-client-secret
export KAKAO_CLIENT_ID=your-kakao-client-id
export KAKAO_CLIENT_SECRET=your-kakao-client-secret
```

## 3. Backend 빌드 및 실행
```bash
cd backend
./gradlew build
./gradlew bootRun
```

## 4. Frontend 빌드 및 실행
```bash
cd frontend
npm install
npm run build
npm run dev
```

## 5. 전체 검증 실행
```bash
./verify/run-all.sh      # 전체 검증 (build + ASVS + contract + security + RBAC)
./verify/run-checklist.sh # 체크리스트 기반 검증
./verify/report-kpi.sh    # KPI 리포트
```

## 6. 커스터마이즈
- `blueprints/auth-manifest.yaml` — 정책값 수정 (rate limit, token expiry 등)
- `blueprints/auth-manifest.yaml#provider_flags` — provider 활성/비활성
- `specs/auth-asvs-l1.yaml` — 검증 항목 추가/수정

## 핵심 파일 구조
```
specs/auth-asvs-l1.yaml        ← 검증 기준 (OWASP ASVS L1)
contracts/auth-openapi.yaml    ← API 계약 (14 endpoints)
blueprints/auth-manifest.yaml  ← 정책 설정
verify/run-all.sh              ← 한 줄 검증
METHODOLOGY.md                 ← 방법론 플레이북
```

## 검증 명령어
| 명령 | 설명 |
|------|------|
| `cd backend && ./gradlew testAsvs` | ASVS 검증만 |
| `verify/run-all.sh` | 전체 검증 |
| `verify/run-checklist.sh` | 체크리스트 검증 |
| `verify/ci-gate.sh` | CI 게이트 (backend + frontend) |
