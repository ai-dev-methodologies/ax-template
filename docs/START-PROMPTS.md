# 시작 프롬프트 — 경로별 복붙용

ax-template으로 **신규 프로젝트를 시작하고 강제 규칙까지 배선**하는 프롬프트 2종.
AI 에이전트(Claude Code 세션)에 그대로 붙여넣어 쓴다.

- 어느 경로인지 아직 못 골랐으면 → **[PLUGIN-CHANNEL.md](PLUGIN-CHANNEL.md)의 비교표**가 정본.
- 절차의 근거 문서: 경로 A = [GETTING-STARTED.md](GETTING-STARTED.md) §3 · 경로 B = [USAGE-GUIDE.md](USAGE-GUIDE.md) §2–5.

> **두 프롬프트의 공통 설계 원칙**: 각 단계에 **관측 가능한 성공 기준**을 붙였고,
> 마지막에 **"강제가 정말 켜졌는지"를 위반을 심어 증명**하는 단계를 넣었다.
> "설치했다"는 자기보고가 아니라 **차단 로그**가 완료 증거다.
> ax-template 자신이 카탈로그에 요구하는 것과 같은 기준이다(green ≠ correct).

---

## 경로 A — fork-as-base (신규 제품 프로젝트, 풀 강제)

**언제**: 새 제품을 처음부터 만든다 · 템플릿 구조(`backend/` + `frontend/`)를 물려받아도 된다 ·
가드/R25/훅 전부 켜고 싶다.

**전제**: JDK 21 · python3 · PyYAML(또는 yq) · node+npm · git ≥ 2.28.
(`/usr/bin/java`는 macOS 껍데기라 실패한다 — `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`)

```
ax-template을 fork-base로 삼아 새 프로젝트 "<PROJECT_NAME>"을 시작한다.
목표 도메인: <한 줄로. 예: 구독 결제가 있는 B2B 관리자 툴>

아래를 순서대로 실행하고, 각 단계는 "성공 기준"이 관측될 때만 다음으로 넘어간다.
기준이 안 나오면 멈추고 무엇이 나왔는지 그대로 보고한다. 추정으로 진행하지 마라.

[1] 클론 + 번들
    git clone https://github.com/ai-dev-methodologies/ax-template <PROJECT_NAME>
    cd <PROJECT_NAME> && git submodule update --init
    성공 기준: 클론 완료. submodule 3개(petclinic/realworld/modulith)는 **선택** —
    이식성 축 검증에만 쓰이고, 가드·게이트·빌드는 서브모듈 없이도 통과한다(실측).
    외부 repo 접근이 막혀 있으면 서브모듈 실패는 무시하고 [2]로 진행한다.

[2] 사전 요구사항 확인 — 없으면 이후 검증이 exit 2로 막힌다
    java -version(21) · python3 -V · python3 -c 'import yaml' · node -v · git --version
    성공 기준: 전부 출력.
    JDK가 21이 아니면 JAVA_HOME부터 잡는다 — macOS의 /usr/bin/java는 껍데기다:
      system/Oracle JDK : export JAVA_HOME=$(/usr/libexec/java_home -v 21)
      Homebrew JDK      : export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
                          (Intel Mac은 /usr/local/opt/...)
    ★ brew로 깐 JDK는 java_home에 등록되지 않으므로 위 첫 명령이 "Unable to locate a
      Java Runtime"으로 실패한다(실측). 그 경우 두 번째 형태를 쓴다.
    python3에 PyYAML이 없고 yq만 있으면 백엔드 전용 실행은 되지만 [9] 전체 실행은
    막힌다 — 전체를 돌릴 거면 PyYAML을 깐다.

[3] 현재 상태 파악 — 이 단계를 건너뛰면 범위를 오판한다
    docs/IMPLEMENTATION-STATUS.md 를 읽고, 25개 L4 도메인을
    full-trio(백엔드+프론트 트리오) / backend-only(서버간 도메인, 프론트 의도적 부재) /
    rules-as-code(INFRA)로 분류해 요약 보고.
    ※ "spec만 있는 도메인"은 더 이상 없다 — 25개 전부 백엔드 구현을 갖는다.
    성공 기준: 내 목표 도메인이 위 셋 중 무엇이며, 프론트가 있는지 없는지 명시됨.

[4] 레시피 선택 (선택이지만 권장)
    recipes/_MANIFEST.yaml 에서 11개 레시피 중 목표에 가장 가까운 것을 고르고
    recipes/<이름>/RECIPE.md 의 "Backend Implementation Status" 표를 읽어
    바로 쓸 수 있는 것 vs 내가 구현해야 하는 것을 표로 보고.

[5] 강제 훅 활성화 — 이걸 해야 커밋/푸시 게이트가 켜진다 (opt-in)
    bash practices/scripts/install-hooks.sh
    성공 기준: git config core.hooksPath 가 .githooks 를 가리킴.

[6] 빌드
    cd backend && ./gradlew build && cd ..
    cd frontend && npm ci && cd ..
    성공 기준: gradle이 `BUILD SUCCESSFUL`, npm이 `added N packages ... in Xs`.
    ※ 이 단계가 가장 오래 걸린다 — 백엔드 빌드는 테스트까지 포함해 10분 안팎이다
      (머신·동시 부하에 따라 더 걸릴 수 있다). 멈춘 게 아니다.

[7] 카탈로그 무결성 확인
    bash practices/evals/run-all-guards.sh
    성공 기준: 전 가드 PASS. FAIL이 있으면 그 이름과 출력을 그대로 보고하고 멈춘다.

[8] ★강제가 정말 막는지 증명 (자기보고 금지 — 실제 차단을 본다)
    bash practices/scripts/ax-prove-gate-blocks-agent.sh
    bash practices/scripts/ax-prove-evidence-gate-blocks-agent.sh
    성공 기준: 두 스크립트 모두 "위반 심음 → 차단됨 → 고침 → 통과" 3단계를 출력.
    차단이 관측되지 않으면 강제가 꺼진 것이다 — 멈추고 보고.

[9] 최종 판정
    bash practices/scripts/verify-completion.sh
    성공 기준: exit 0. exit 1이면 출력된 fix_playbook을 적용하고 재실행.
    exit 2는 도구 누락이다 — [2]로 돌아간다.

[10] 이후 개발 규율 (이 프로젝트의 상시 규칙으로 삼는다)
    - 새 도메인은 코드가 아니라 spec부터. METHODOLOGY.md 5단계 +
      docs/NEW-DOMAIN-CHECKLIST.md 의 필수 산출물을 빠짐없이 만든다
      (entity·repo·service·thin controller·state machine·domain advice·V###.sql·
       ComplianceTest·ViolationProofTest·per-domain gradle task).
    - 새 규칙은 evidence 없이 못 만든다 — 외부 URL/인용이 있어야 게이트를 통과한다.
    - "완료"는 verify-completion.sh exit 0 으로만 선언한다.

먼저 [1]~[3]을 실행하고 결과를 보고한 뒤 진행 승인을 받아라.
```

### 경로 A에서 실제로 켜지는 강제

| 시점 | 검사 | 활성 조건 |
|---|---|---|
| 커밋 | `practices/` 변경 시 4 하드게이트(spec_ref·substance·evidence·time_decay) | [5] 실행 |
| 푸시 | HEAD에 대한 최근 R25 통과 기록 + 회귀 | [5] 실행 |
| 완료 선언 전 | R25 전체 | 항상 수동 실행 |
| CI | — | fork 팀 자율 (템플릿이 강제하지 않음) |

---

## 경로 B — plugin 채널 (기존 프로젝트 / 자유 레이아웃 / fork 불원)

**언제**: 이미 있는 프로젝트다 · 디렉토리 구조가 템플릿과 다르다 · 카탈로그(지식)만 얹고
강제는 원하는 것만 고르고 싶다.

**전제**: Claude Code CLI + 네트워크. **JDK/node는 이 단계에선 필요 없다**
(기계 강제를 실제로 배선할 때 해당 스택 도구만 필요).

```
이 프로젝트에 ax-template 카탈로그를 plugin 채널로 얹는다.
프로젝트 성격: <한 줄. 예: Next.js 프론트 + Spring Boot API, 사내 관리자 툴>

아래를 순서대로. 각 단계의 "성공 기준"이 관측될 때만 다음으로 간다.

[1] 설치 (이 계정에서 1회면 됨 — 모든 프로젝트에 적용)
    claude plugin marketplace add ai-dev-methodologies/ax-template
    claude plugin install ax-transform@ax-transform
    claude plugin list
    성공 기준: ax-transform@ax-transform · enabled.
    주의: 진행 중인 세션에는 반영되지 않는다. 새 세션을 열어야 스킬이 뜬다.
    스킬은 ax-transform: 네임스페이스로 등록된다(예: /ax-transform:ax-practices).

[2] 프로젝트 선언 (프로젝트당 1회)
    프로젝트 루트에서 새 세션을 열고: /ax-init-config
    성공 기준: 감지된 스택·경로가 담긴 ax.config.json 초안이 제시되고,
    내가 승인한 뒤에 파일이 생성된다(승인 없이 생성되면 잘못된 것이다).
    확인 포인트 — 내 실제 레이아웃과 맞는가:
      · react.root / java.root 가 실제 코드 위치인가
      · srcDir 이 내 소스 디렉토리명인가 (단일 세그먼트만 허용)
      · alias 가 내 tsconfig/vite alias 와 일치하는가
      · layers 의 디렉토리명 배열이 내 구조와 맞는가
        (app > features > shared 3계층 자체는 불변식이라 못 바꾼다)
    틀리면 그 자리에서 고쳐 승인한다 — 여기가 틀리면 이후 lint가 조용히 0위반이 된다.

[3] 일상 사용 (지식 레이어)
    코딩/리뷰 중: /ax-practices
    성공 기준: 지적마다 룰 id(ax/<id> 또는 practices/rules/<id>.md)가 인용된다.
    인용 없는 지적은 카탈로그 근거가 없는 것이므로 받아들이지 않는다.
    이 단계에서 스킬은 gradle/eslint 기계 검증을 대행하지 않는다 —
    "미설치" 안내가 나오면 그건 정상이고, 원하면 [4]로 간다.

[4] ★기계 강제 배선 (원하는 것만 — 여기서부터가 실제 차단)
    React:  /ax-install-react-enforcement     (node/npm + ESLint 9 필요)
    Java:   /ax-install-java-enforcement      (JDK + gradle 필요)
    훅:     /ax-install-hooks                 (git)
    성공 기준: react/java 설치 스킬은 배선 직후 probe(위반 심기 → 검출 확인 → 삭제)까지
    스스로 수행한다. 단 **관측물이 스택마다 다르다**:
      react — lint 출력에 룰 id(예: `ax/no-upward-layer-import`)가 뜬다
      java  — 레이어 경계 테스트가 **RED로 실패**하고 실패 출력에 probe 클래스명이 뜬다
              (룰 id가 아니다 — 여기서 룰 id를 찾으면 못 찾는다)
    probe에서 위반이 검출되지 않으면 배선이 공허한 것이다 — 그 자리에서 멈추고 보고한다.
    ⚠️ ax-template 레포의 .githooks/ 를 복사하지 마라 — pre-push recency guard는
       그 레포 전용이라 이 프로젝트의 모든 push를 영구 차단한다.

[5] 훅 배선했다면 직접 확인
    /ax-install-hooks 는 probe가 없으므로, 위반 커밋을 한 번 시도해
    차단되는 것을 눈으로 확인하고 되돌린다.

[6] 갱신이 필요할 때 (중요 — update 명령은 동작하지 않는다)
    claude plugin marketplace update ax-transform
    claude plugin uninstall ax-transform@ax-transform
    claude plugin install ax-transform@ax-transform
    이유: plugin.json 버전이 0.1.0에 고정돼 있어 updater가 no-op한다(실측).
    재설치가 현재 유일하게 검증된 갱신 경로다.

먼저 [1]~[2]를 실행하고, 생성된 ax.config.json 을 보여준 뒤 진행 승인을 받아라.
```

### 경로 B에서 켜지는 것 / 안 켜지는 것

| | 상태 |
|---|---|
| 카탈로그 지식 라우팅(`/ax-practices`) | [1][2]만으로 동작 |
| ESLint 15룰 실제 차단 | [4] React 설치 후 |
| ArchUnit 시작 체크 3종 | [4] Java 설치 후 |
| pre-commit 훅 | [4] hooks 설치 후 |
| **가드 108종 · R25 · 4 하드게이트** | **이식되지 않는다** — ax-template 자신의 자기검증 체계다 |

경로 B는 "카탈로그를 소비"하는 채널이지 "ax-template의 자기검증을 복제"하는 채널이 아니다.
풀 강제가 목적이면 경로 A다.

---

## 두 경로를 섞어도 되나

된다. 흔한 조합은 **경로 A로 새 제품을 만들면서, 같은 계정의 다른 기존 프로젝트에는
경로 B를 얹는 것**이다. 플러그인 설치는 계정 단위(user scope)라 경로 A 레포 안에서
여는 세션도 스킬은 설치된 플러그인을 통해 쓴다(레포 루트의 `skills/`는 Claude Code가
직접 로드하는 경로가 아니다).
