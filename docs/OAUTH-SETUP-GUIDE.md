# OAuth Provider 키 발급 가이드

이 템플릿의 SNS 로그인(Google, Kakao, Naver)을 동작시키려면 각 provider에서 OAuth 앱을 등록하고 키를 발급받아야 합니다. 3개 모두 **무료**이며, 각각 **5분 이내**에 완료됩니다.

---

## 1. Google OAuth 설정

### 1-1. Google Cloud Console 접속
- https://console.cloud.google.com/ 접속 → Google 계정 로그인

### 1-2. 프로젝트 생성
- 상단 프로젝트 선택 → "새 프로젝트" → 이름 입력 (예: `ax-template-dev`) → 만들기

### 1-3. OAuth 동의 화면 설정
- 좌측 메뉴: **APIs & Services > OAuth consent screen**
- User Type: **External** 선택
- 앱 이름, 사용자 지원 이메일 입력
- **Test users**: 본인 Gmail 추가 (테스트 모드에서는 등록된 사용자만 로그인 가능)
- 저장

### 1-4. OAuth 2.0 클라이언트 ID 생성
- 좌측 메뉴: **APIs & Services > Credentials**
- **+ CREATE CREDENTIALS > OAuth client ID**
- Application type: **Web application**
- **Authorized redirect URIs** 추가:
  ```
  http://localhost:8080/api/auth/oauth/google/callback
  ```
- 만들기 → **Client ID**와 **Client Secret** 복사

### 1-5. 환경변수 설정
```bash
export GOOGLE_CLIENT_ID="복사한-client-id"
export GOOGLE_CLIENT_SECRET="복사한-client-secret"
```

---

## 2. Kakao OAuth 설정

### 2-1. Kakao Developers 접속
- https://developers.kakao.com/ 접속 → Kakao 계정 로그인

### 2-2. 애플리케이션 추가
- **내 애플리케이션 > 애플리케이션 추가하기**
- 앱 이름 입력 (예: `ax-template-dev`) → 저장

### 2-3. 플랫폼 등록
- 좌측 메뉴: **앱 설정 > 플랫폼**
- **Web 플랫폼 등록** → 사이트 도메인:
  ```
  http://localhost:8080
  ```

### 2-4. 카카오 로그인 활성화
- 좌측 메뉴: **제품 설정 > 카카오 로그인**
- **활성화 설정**: ON
- **Redirect URI 등록**:
  ```
  http://localhost:8080/api/auth/oauth/kakao/callback
  ```

### 2-5. 동의항목 설정
- 좌측 메뉴: **제품 설정 > 카카오 로그인 > 동의항목**
- **닉네임**: 필수 동의
- **카카오계정(이메일)**: 선택 동의 (또는 필수)

### 2-6. 키 확인
- 좌측 메뉴: **앱 설정 > 앱 키**
- **REST API 키** = Client ID
- 좌측 메뉴: **제품 설정 > 카카오 로그인 > 보안** → **Client Secret** 발급 + 활성화

### 2-7. 환경변수 설정
```bash
export KAKAO_CLIENT_ID="REST-API-키"
export KAKAO_CLIENT_SECRET="발급받은-시크릿"
```

---

## 3. Naver OAuth 설정

### 3-1. Naver Developers 접속
- https://developers.naver.com/ 접속 → Naver 계정 로그인

### 3-2. 애플리케이션 등록
- **Application > 애플리케이션 등록**
- 애플리케이션 이름 입력 (예: `ax-template-dev`)
- **사용 API**: 네이버 로그인 선택
- **제공 정보 선택**: 이메일, 이름, 프로필 사진

### 3-3. 환경 설정
- **로그인 오픈 API 서비스 환경**: PC 웹
- **서비스 URL**:
  ```
  http://localhost:8080
  ```
- **Callback URL**:
  ```
  http://localhost:8080/api/auth/oauth/naver/callback
  ```

### 3-4. 키 확인
- 등록 완료 후 **Client ID**와 **Client Secret** 표시됨 → 복사

### 3-5. 환경변수 설정
```bash
export NAVER_CLIENT_ID="복사한-client-id"
export NAVER_CLIENT_SECRET="복사한-client-secret"
```

---

## 4. 전체 환경변수 한번에 설정

`.env.local` 파일을 프로젝트 루트에 생성:

```bash
# .env.local (절대 git에 커밋하지 마세요)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
KAKAO_CLIENT_ID=your-kakao-rest-api-key
KAKAO_CLIENT_SECRET=your-kakao-client-secret
NAVER_CLIENT_ID=your-naver-client-id
NAVER_CLIENT_SECRET=your-naver-client-secret
```

적용:
```bash
source .env.local
cd backend && ./gradlew bootRun
```

---

## 5. 키 없이 실행하면?

키를 설정하지 않으면 이메일 로그인은 정상 동작하지만, OAuth 로그인 시 provider로 리다이렉트 후 인증 실패합니다.

### 키 상태 확인 명령
```bash
./verify/check-oauth-keys.sh
```

### 예상 출력 (키 미설정 시):
```
=== OAuth Key Status ===
  ❌ GOOGLE_CLIENT_ID: not set (using dummy)
  ❌ KAKAO_CLIENT_ID: not set (using dummy)
  ❌ NAVER_CLIENT_ID: not set (using dummy)

OAuth 로그인을 사용하려면 docs/OAUTH-SETUP-GUIDE.md를 참조하세요.
```

---

## 6. 검증

키 설정 후 OAuth 연동 확인:
```bash
# 1. 백엔드 시작
cd backend && ./gradlew bootRun

# 2. 브라우저에서 직접 테스트
# Google: http://localhost:8080/api/auth/oauth/google/authorize
# Kakao:  http://localhost:8080/api/auth/oauth/kakao/authorize
# Naver:  http://localhost:8080/api/auth/oauth/naver/authorize

# 3. 로그인 완료 후 콜백 → JWT 발급 확인
```
