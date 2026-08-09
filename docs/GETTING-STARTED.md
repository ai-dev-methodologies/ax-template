# Getting Started — ax-template 처음 쓰는 사람을 위한 안내

> 이 문서 하나로 세 가지에 답한다: **① 이걸로 뭘 할 수 있나 ② 어떻게 만드나 ③ 결과가 어떻게 나오나.**
> 깊이 들어가려면: 방법론 `METHODOLOGY.md` · 새 도메인 산출물 `docs/NEW-DOMAIN-CHECKLIST.md` · 프로젝트 규칙 `CLAUDE.md`.

---

## 1. 이게 뭔가 — 한 문장

**React(Next.js) + Spring Boot 프로젝트를 fork로 시작하되, "AI가 규칙 밖 코드를 쓰면 기계가 막는" 강제 장치가 처음부터 깔려 있는 템플릿**이다.

일반 보일러플레이트와 다른 점은 코드가 아니라 **강제 장치**다:

| 일반 템플릿 | ax-template |
|---|---|
| 예제 코드를 준다 | 예제 코드 + **그 코드가 지켜야 할 규칙**을 준다 |
| 규칙은 문서(README)에 적혀 있다 | 규칙은 **스크립트가 검사**한다 — 어기면 커밋/푸시가 실패한다 |
| AI가 규칙을 안 지켜도 사람이 리뷰에서 잡아야 한다 | AI가 규칙을 어기면 **사람 리뷰 전에 게이트가 차단**한다 |
| "이렇게 하세요" | "이렇게 안 하면 빌드가 안 됩니다" |

핵심 가정: **AI 에이전트(Claude Code, Cursor 등)가 타이핑의 대부분을 한다.** 그러면 문제는 속도가 아니라 **환각·낡은 튜토리얼 패턴·프레임워크 버전 드리프트**다. 이 템플릿은 그걸 코드 리뷰가 아니라 기계로 잡는다.

---

## 2. 뭘 할 수 있나 — 3가지 사용법

### (A) 새 프로젝트를 fork로 시작한다 (가장 흔한 경우)

fork하는 순간 이미 들어있는 것:

- **동작하는 참조 구현 25개 도메인** — 인증(OAuth 포함) · CRUD · 결제 · 알림 · 감사로그 · 파일저장 · 검색 · 권한 · 세션 · 승인결재 · 리포트 내보내기 · API 키 … (`specs/` 168개 spec 파일)
- **규칙 카탈로그** — Java/Spring 233룰(`practices/rules/`) + React 102룰(`practices-react/rules/`) + ESLint 15룰(기계 검사)
- **강제 장치 108개** — 하드 가드 스크립트. 커밋·푸시·완료선언 시점에 돌아간다
- **AI 진입점** — `AGENTS.md`(에이전트가 처음 읽는 파일, 룰 소스에서 자동 생성 + sha256으로 stale 감지)

즉 "결제 붙여줘"라고 하면 AI가 **백지에서 발명하지 않고** 기존 payment 도메인의 spec·테스트·규칙을 따라간다.

### (B) 새 도메인을 규칙대로 추가한다

정해진 플레이북(5단계)이 있고, 그 단계를 **건너뛰면 게이트가 막는다**. → §4

### (C) 기존 프로젝트를 AI 친화적으로 전환한다

`/ax-transform` 스킬(이 repo가 그 스킬의 소스)이 spec-first 구조 + 규칙 카탈로그 + 검증 루프를 기존 코드베이스에 이식한다.

---

## 3. 설치 — 30분

### 3-1. 사전 요구사항 (없으면 검증이 exit 2로 **차단**된다)

| 도구 | 언제 필요 | 확인 |
|---|---|---|
| **JDK 21** | 백엔드/gradle 단계 | `java -version` → 21. macOS의 `/usr/bin/java`는 껍데기라 실패한다. system/Oracle JDK: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` — Homebrew JDK(`brew install openjdk@21`)는 java_home에 자동 등록되지 않으므로 `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`(Intel Mac은 `/usr/local/opt/...`) |
| **python3** | 항상 (체크리스트 파싱·가드 헬퍼) | `python3 -V` |
| **PyYAML 또는 yq** | 체크리스트 파싱(항상) | `python3 -c 'import yaml'` 또는 `yq --version` — 단 catalog-guard 단계(전체 실행 포함)는 **PyYAML이 필수**, yq는 대체 불가(가드가 `import yaml`을 직접 embed하고 없으면 조용히 SKIP한다) |
| **node + npm** | 프론트 lint 단계에서만 | `node -v` (백엔드만 돌릴 땐 없어도 안 막힌다) |

> 이 preflight는 **조용히 건너뛰지 않는다.** 도구가 없으면 `verify-completion.sh`가 exit 2로 멈추고 무엇이 없는지 말한다. (과거에 PyYAML이 없어서 테스트 fixture가 "의도한 이유가 아닌 이유로" 통과하던 사고가 있었고, 그래서 막게 만들었다.)

### 3-2. 클론 → 훅 설치 → 빌드

```bash
git clone https://github.com/<your-org>/ax-template.git my-project
cd my-project
git submodule update --init   # portability fixtures(petclinic/realworld/modulith)용 — 가드·게이트는
                               # 서브모듈 없이도 통과한다(필수 아님, 이식성 축 검증에만 필요)

# ① 강제 훅 활성화 (opt-in — 이걸 해야 커밋/푸시 게이트가 켜진다)
bash practices/scripts/install-hooks.sh

# ② 백엔드
cd backend && ./gradlew build && cd ..

# ③ 프론트엔드 (lockfile이 커밋돼 있으므로 npm ci — 재현 가능한 설치)
cd frontend && npm ci && cd ..
```

### 3-3. 잘 깔렸는지 확인 — 한 줄

```bash
bash practices/scripts/verify-completion.sh
```

이게 **이 프로젝트의 유일한 "다 됐다" 판정 기준**이다(R25). 뒤에서 자세히 본다.

---

## 4. 어떻게 만드나 — 새 도메인 추가 (예: 쿠폰)

### 원칙: 코드가 아니라 **spec부터**

```
/ax-scaffold coupon      ← 빈 Spec Trio 스켈레톤 생성 (# TODO 마커가 박힌 채로)
        ↓
/ax-plan coupon          ← 인터뷰하며 spec을 채우고, spec 항목 1개당 실패하는(RED) 테스트 1개 생성
        ↓
구현 (AI가 작성)          ← RED → GREEN
        ↓
./gradlew testCoupon     ← 이 도메인만 검증
        ↓
verify-completion.sh     ← 전체 게이트 (R25)
```

**스캐폴드만 하고 계획을 건너뛰면 빌드가 막힌다.** `spec_scaffold_unfilled_guard.sh`가 `# TODO: Add` 마커가 남은 spec을 발견하면 BLOCK한다. "빈 껍데기 spec으로 게이트를 통과"하는 편법을 기계가 차단하는 것이다.

### Spec Trio — 도메인 하나당 3개 문서

| 파일 | 역할 |
|---|---|
| `specs/coupon-l0.yaml` | **검증 항목 목록.** 항목 하나 = `@Tag` 테스트 하나 (1:1 강제) |
| `contracts/coupon-openapi.yaml` | API 계약. 엔드포인트의 진실 |
| `blueprints/coupon-manifest.yaml` | 정책값 (만료일, 한도, 임계치…) |

**코드가 아니라 spec이 진실이다.** 구현은 spec의 참조 구현일 뿐이다.

### 규칙을 새로 추가할 때 — "근거 없는 규칙"은 못 만든다

`practices/rules/*.md`의 모든 규칙은 `evidence:` 블록이 필수다. 저장된 외부 스냅숏의 **실제 인용문**(`upstream_id`) 또는 RFC/JEP/공식문서 **URL + 인용**(`source_type: external`) 중 하나여야 한다.

- `evidence_guard.sh` — 빈 evidence·placeholder를 **커밋 시점에 차단**(구조 검사)
- `evidence_quote_spotcheck_guard.sh` — `upstream_id` 인용문이 스냅숏 본문에 **실제로 존재하는지** 대조(내용 검사)

→ AI가 "일반적으로 이렇게 합니다" 같은 근거 없는 규칙을 만들어 넣기 어렵다.

> **정직한 한계:** `source_type: external`(외부 URL) 형태는 **구조만** 기계 검사된다. URL이 실제로 그 내용을 담고 있는지는 네트워크가 필요해서 `external_url_spot_audit.sh`가 **주기적 advisory**로 표본 검사한다 — 커밋을 막지는 않는다. 즉 "스냅숏에 저장된 인용"은 기계가 대조하고, "외부 URL 인용"은 사람이 표본으로 감사한다.

---

## 5. 결과가 어떻게 나오나 — 실제 출력

### 5-1. 도메인 하나 검증 — binary pass/fail

```console
$ cd backend && ./gradlew testCrud

> Task :testCrud
BUILD SUCCESSFUL in 15s
```

실패하면 어떤 spec 항목이 깨졌는지 `@Tag`로 특정된다. **"대충 돌아가는 것 같다"가 없다 — 통과 아니면 실패다.**

### 5-2. 가드 — 규칙 위반 차단

```console
$ bash practices/evals/private_boundary_guard.sh
private_boundary_guard: PASS — no private boundary violations
  (scanned: backend/src frontend/src specs contracts blueprints practices/rules docs README.md CLAUDE.md .github)
```

위반이 있으면 exit 1 + 어느 파일 몇 번째 줄인지 출력하고, **커밋이 실패한다.**

### 5-3. 완료 판정 (R25) — 이게 최종 게이트

```console
$ bash practices/scripts/verify-completion.sh

=== verify-completion.sh — Summary ===
  PASS         : 10
  WARN(advisory): 0
  FAIL         : 0
  SKIP         : 0

verify-completion: PASS — all steps green. Task may declare done.
```

돌아가는 단계(8 step): `backend-build` → `structural-pregate` → `per-domain-tests`(도메인별 gradle 태스크 전수) → `hard-guards`(108개) → `catalog-meta-guards` → `frontend-lint` → `frontend-test` → `aggregate-regression`

**exit 0이어야 "완료"라고 말할 수 있다.** 실패하면 `fix_playbook`(고치는 법)이 출력된다. AI 에이전트도 사람도 이 규칙을 우회할 수 없다 — `--skip` 플래그가 없고, 푸시할 때 훅이 "최근 R25 통과 기록이 HEAD에 있는지" 다시 확인한다.

### 5-4. 무엇이 언제 막나

| 시점 | 무엇이 검사되나 | 활성화 |
|---|---|---|
| **커밋** | `practices/` 변경 시 4개 하드 게이트(spec_ref·substance·evidence·time_decay) + 커밋 메시지 스캔 | `install-hooks.sh` 실행한 클론 |
| **푸시** | HEAD에 대한 최근 R25 통과 기록(모든 푸시) + 전체 회귀(testPractices/testAsvs/testCrud, `backend/`·`practices/`·seed spec을 건드리는 diff일 때만) | 동일 |
| **완료 선언 전** | R25 전체 (위 8 step) | 항상 수동 실행 가능 |
| **CI** | 카탈로그 품질 프로브 | fork 팀 자율 |

> 이 훅들은 **opt-in**이다 — fork받은 팀이 켤지 말지 정한다. 템플릿은 카탈로그 품질만 보장하고, git·배포·리뷰 정책은 강제하지 않는다.

---

## 6. "정말 막히나?" — 직접 확인하는 법 (falsification)

이 템플릿의 주장은 **반증 가능하게** 설계돼 있다. 믿지 말고 돌려보면 된다:

```bash
# AI 에이전트가 규칙 위반 코드를 쓰면 → 차단 → 고치면 → 통과, 이 3단계를 실제로 실행해 보여준다
bash practices/scripts/ax-prove-gate-blocks-agent.sh
bash practices/scripts/ax-prove-evidence-gate-blocks-agent.sh
```

또한 모든 가드는 **pass fixture / fail fixture 쌍**을 갖고 있고, "가드가 항상 통과만 하는 껍데기(vacuous)"가 아님을 증명하는 메타 가드(`fixture_kill_proof_guard.sh`)가 별도로 돈다 — 가드의 탐지 로직을 일부러 무력화하면 fail fixture가 정말 통과로 뒤집히는지 변이(mutation)로 확인한다.

---

## 7. 커스터마이즈 — 어디를 건드리나

| 하고 싶은 것 | 건드릴 곳 |
|---|---|
| 정책값 변경 (토큰 만료, rate limit 등) | `blueprints/*-manifest.yaml` |
| 검증 항목 추가/수정 | `specs/*.yaml` (+ 대응하는 `@Tag` 테스트) |
| OAuth 공급자 켜기/끄기 | `blueprints/auth-manifest.yaml#provider_flags` |
| OAuth 키 발급 | `docs/OAUTH-SETUP-GUIDE.md` (Google/Kakao/Naver 각 5분, 무료) |
| 회사 고유 식별자·시크릿 | **이 트리에 넣지 않는다.** SPI 인터페이스만 public, 구현은 fork에 (R26 — `private_boundary_guard`가 검사) |

---

## 8. 자주 막히는 지점 (실제 사고 기록)

| 증상 | 원인 | 해결 |
|---|---|---|
| `Unable to locate a Java Runtime` | macOS `/usr/bin/java` 껍데기, 또는 `java_home`이 Homebrew JDK를 못 찾음 | system/Oracle JDK: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` — Homebrew JDK: `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`(Intel Mac: `/usr/local/opt/...`) |
| R25가 `ModuleNotFoundError: yaml`로 실패 | PyYAML 없음 | 체크리스트 파싱만이면 `brew install yq`로도 통과하지만, catalog-guard 단계가 포함된 전체 실행은 yq로 대체 불가 — `pip3 install pyyaml` |
| `npm ci` 실패 | lockfile과 package.json 불일치 | `npm install --package-lock-only` 후 재시도 |
| 푸시가 "R25 audit log 없음"으로 거부 | 커밋 후 R25를 안 돌림 | HEAD에서 `verify-completion.sh` 재실행 후 푸시 |
| 커밋이 evidence 게이트에서 거부 | 새 규칙에 근거(인용/URL)가 없음 | 실제 외부 출처를 anchor. **placeholder는 통과 못 한다** |

---

## 9. 다음에 읽을 것

| 문서 | 언제 |
|---|---|
| `METHODOLOGY.md` | 새 도메인 만들기 직전 (5단계 플레이북) |
| `docs/NEW-DOMAIN-CHECKLIST.md` | "이 도메인에 뭘 다 만들어야 하지?" (필수 산출물 목록) |
| `CLAUDE.md` | AI 에이전트가 이 repo에서 지켜야 할 규칙 전문 |
| `docs/BACKLOG.md` | 이 카탈로그의 미완성 부분 (정직하게 공개돼 있다) |
| `skills/ax-transform/SKILL.md` | 기존 프로젝트에 이 방식을 이식할 때 |
