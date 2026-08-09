# 소비 경로 가이드 — fork-as-base vs plugin 채널 (D-track)

ax-template의 카탈로그(Java 233룰 · React 102룰 · ESLint 15룰 · 검증 자산)를 소비하는 경로는
**두 개**다. 프로젝트 성격에 따라 고른다.

| | 경로 A — fork-as-base | 경로 B — plugin 채널 |
|---|---|---|
| 대상 | 신규 제품 프로젝트, 풀 강제 원함 | 기존 프로젝트 / 자유 레이아웃 / fork 불원 |
| 시작 방법 | 레포 clone → 그 위에 개발 | `/plugin marketplace add` → `/plugin install` |
| 레이아웃 | 템플릿 구조(`src/` · `@/` · `com.ax.template`) 상속 | `ax.config.json`으로 자기 레이아웃 선언 |
| 강제 수준 | hard gates + guards + R25 전부 자동 | 지식 레이어 기본 + 강제는 opt-in 설치 |
| 참조 워크로드 | 포함(레퍼런스로 활용) | 미포함(카탈로그만 소비) |

경로 A는 README/CLAUDE.md의 기존 문서가 정본이다. 이 문서는 **경로 B**를 다룬다.

---

## 경로 B — 3단계 + 선택 1단계

### 1단계 — 설치 (머신당 1회)

```
/plugin marketplace add ai-dev-methodologies/ax-template
/plugin install ax-transform@ax-transform
```

레포 자체가 marketplace다(`.claude-plugin/marketplace.json`, 별도 dist 레포 없음 — R109).
설치되면 스킬 26종이 Claude Code에 로드된다. 이 채널의 핵심 스킬 5종:

| 스킬 | 역할 |
|---|---|
| `/ax-init-config` | 프로젝트 감지 → `ax.config.json` 초안 제시 → **사용자 승인 후에만 생성** |
| `/ax-practices` | 진입/라우팅 — INDEX 기반으로 룰을 선별·적용 (지식 레이어) |
| `/ax-install-react-enforcement` | ESLint 플러그인 `file:` 설치 + `settings.ax` 배선 (기계 강제) |
| `/ax-install-java-enforcement` | ArchUnit 시작 체크 3종 배선 (기계 강제, 소수만) |
| `/ax-install-hooks` | pre-commit 훅 배선 (core.hooksPath / husky / lefthook) |

### 2단계 — 프로젝트 선언 (프로젝트당 1회)

대상 프로젝트 루트에서 `/ax-init-config`. package.json · build.gradle(.kts) · 디렉터리
구조를 감지해 완성된 config를 보여주고, 승인하면 `ax.config.json`을 만든다. 예:

```json
{
  "version": 1,
  "stacks": ["react", "java"],
  "react": {
    "root": ".",
    "srcDir": "source",
    "alias": { "#/": "source/" },
    "layers": { "app": ["pages"], "features": ["modules"], "shared": ["core", "ui"] }
  },
  "java": { "root": "backend", "buildTool": "gradle", "rootPackage": "com.mycompany.app" }
}
```

- `srcDir`/`alias`/`layers`가 전부 커스터마이즈 가능 — `src/`·`@/`가 아니어도 된다.
- `srcDir`는 **단일 세그먼트**만 허용된다 — 스키마(`pattern: ^[^/]+$`)로 제약하고,
  **런타임(`layoutFrom()`)도** `/`가 포함된 값을 즉시 `Error` throw로 거부한다(ESLint
  프로세스가 fatal error로 죽는다, exit code 2). 스키마는 손편집·생성기 결함으로 우회될
  수 있으므로 — 다중 세그먼트가 스키마를 우회해 그대로 통과하면 `classifySrcPath`가
  모든 파일을 `layer: null`로 분류해 4개 레이어 룰 전부가 조용히 0위반이 되는 silent-miss가
  발생한다 — 스키마만으로는 이 결함을 막지 못한다. 그래서 두 층 모두 강제한다.
- 레이어 **개수/이름**(app > features > shared 3계층 단방향)은 불변식이라 바꿀 수 없다 —
  변수화 대상은 각 레이어의 **디렉터리명 배열**뿐이다.
- 스키마 정본: `practices-react/eslint-plugin-ax/schemas/ax.config.schema.json` ·
  샘플: 레포 루트 `ax.config.sample.json`.

### 3단계 — 일상 사용 (지식 레이어)

코딩/리뷰 중 `/ax-practices`. 절차는 스킬에 내장돼 있다:

1. `ax.config.json` 읽기 (없으면 `/ax-init-config`로 위임하고 중단 — 추정 진행 금지)
2. `stacks`에 해당하는 카탈로그 INDEX만 로드 (`practices/INDEX.md` ·
   `practices-react/INDEX.md` — 자동 생성 인덱스, 전체 카탈로그 2.7MB를 읽지 않는다)
3. 작업 맥락·태그 대조로 룰 선별 — impact 총순서
   `CRITICAL > HIGH > MEDIUM-HIGH > MEDIUM > LOW-MEDIUM > LOW`, **최대 8룰**만 본문 로드
4. `verification_kind: review` 룰만 직접 적용. 그 외(gradle/eslint/guard 등)는
   **"미설치 — 설치 가이드 참조"** 안내만 한다 — 스킬은 기계 검증을 대행했다고
   주장하지 않는다 (deny-by-default 라우팅)
5. 모든 지적에 룰 id 인용 (`ax/<id>` 또는 `practices/rules/<id>.md`)
6. `react.root`/`java.root` 밖 파일에는 카탈로그를 적용하지 않는다

### 선택 4단계 — 기계 강제 배선 (원할 때만)

강제층은 스킬로 나를 수 없다 — guard들은 REPO_ROOT를 자기 위치 기준으로 해석하므로
대상 프로젝트에 **설치**돼야만 작동한다(R109). 그래서 설치 가이드 스킬이 분리돼 있다:

- **`/ax-install-react-enforcement`** — 플러그인 `file:` 설치 + `eslint.config.mjs`에
  `settings: { ax: axConfig.react }` 주입. `files` 글롭은 `srcDir` 파라미터화(하드코딩
  금지 — 커스텀 srcDir에서 조용히 0위반이 되는 결함을 막는다). TypeScript 프로젝트는
  `typescript-eslint` 설치 + parser 배선도 같은 블록에 필수 — 없으면 `.ts`/`.tsx` 전체가
  parsing error로 스킵돼 모든 ax 룰이 조용히 미실행된다. 설치 직후 **probe 절차**
  (위반 파일 심기 → 룰 id 검출 확인 → 삭제)로 배선이 공허하지 않음을 그 자리에서 증명.
- **`/ax-install-java-enforcement`** — archunit-junit5 + `rootPackage` systemProperty
  변수화 `testPractices` task + 시작 체크 3종(layer-boundary / no-cyclic / DTO-record).
  정직한 스코프: Java 룰 다수는 review형이라 기계화 대상이 아니다 — 그건 `/ax-practices`
  소관이다.
- **`/ax-install-hooks`** — pre-commit만 배선. ⚠️ **ax-template의 `.githooks/`를 복사하지
  마라** — pre-push recency guard는 이 레포 전용이라 대상 프로젝트의 모든 push를 영구
  차단한다.

---

## 이 채널이 보장하는 것 / 하지 않는 것

**보장**: 설치하면 동작한다 + 동작하지 않는 조건이 진단 가능하다(각 설치 스킬의 probe
검증과 4단계 진단 순서). 레이아웃 주입이 실제로 판정을 바꾼다는 것은 대조군 포함
외부 검증으로 실증됐다(위반 검출 → `settings.ax` 제거 시 동일 파일 셋에서 0건).

**보장하지 않음**: guard 108종·R25·pre-commit 4-gate 같은 카탈로그 자기검증 체계는
ax-template 자신의 것이고 대상 프로젝트로 이식되지 않는다. fork-receiver의 CI/merge
gate/branch protection은 자율 영역이다(기존 원칙 그대로).

## 유지보수 노트 (maintainer용)

- INDEX는 룰 추가/수정 후 `bash practices/generate_index.sh --catalog <practices|practices-react>`
  로 **수동 재생성**한다 (generate_agents.sh 배선 없음 — disk-truth guard 2종이 guard 실행
  중 generate_agents.sh를 재실행하므로 배선하면 게이트가 트리를 오염시킨다. staleness는
  P3 doc-drift로 수용, `practices/MAINTAINER.md` 주기 작업 참조).
- 21st.dev 파생 블록(`@ax-codified-from`)은 `templates/DERIVED-SOURCES.yaml` provenance
  대장으로 관리되고 `derived_block_license_guard`가 등재 누락·헤더 불일치를 차단한다.
- 결정 이력: `practices/DECISIONS.md` R109.
