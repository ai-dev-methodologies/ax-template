# BOOTSTRAP-SKILL — plugin 채널(경로 B) 자동 구성 지시문

> **이 문서는 AI 에이전트(Claude Code 세션)가 읽고 그대로 실행하는 지시문이다.**
> 사람에게 주는 설명문은 [USAGE-GUIDE.md](USAGE-GUIDE.md)·[PLUGIN-CHANNEL.md](PLUGIN-CHANNEL.md)가 정본.
> 전달용 짧은 프롬프트는 [START-PROMPTS.md](START-PROMPTS.md) 참조.

---

너는 지금 열려 있는 프로젝트에 ax-template 카탈로그를 **설치된 스킬로만** 연결한다.
레포를 clone하거나 fork하지 않는다. 파일을 복사해 오지도 않는다.
이 프로젝트에 생기는 것은 `ax.config.json` 하나와, 사용자가 명시적으로 승인한 강제 배선뿐이다.

## 규칙 (전 단계 공통)

- 각 단계의 "성공 기준"이 **관측될 때만** 다음으로 간다. 안 나오면 멈추고 출력 원문
  그대로 보고한다. 추정으로 진행하지 마라.
- 필요한 지식은 **전부 설치된 스킬 안에 있다.** 외부에서 ax-template 레포를 clone하거나
  그 파일을 이 프로젝트로 복사하지 마라. 스킬이 안내하지 않는 것을 임의로 만들지 마라.
- 프로젝트에 대한 정보(성격·스택·구조)는 **네가 직접 조사하고, 조사로 확정 안 되는
  것만 사용자에게 물어라.** 미리 받은 정보는 없다.
- 스킬 설명과 실제가 다르거나 막히면 **우회하지 말고 기록**한다 (아래 [F]).

## [0] 스킬 연결 확인 (확인만 — 설치·갱신은 네 담당이 아니다)

이 세션에 아래 5종 스킬이 로드돼 있는지 확인:

- `/ax-transform:ax-init-config`
- `/ax-transform:ax-practices`
- `/ax-transform:ax-install-react-enforcement`
- `/ax-transform:ax-install-java-enforcement`
- `/ax-transform:ax-install-hooks`

성공 기준: 5종 모두 사용 가능 목록에 존재. 없으면 멈추고 사용자에게 보고한다 —
설치 절차는 [USAGE-GUIDE.md](USAGE-GUIDE.md) §2 (계정당 1회, `claude plugin marketplace add
ai-dev-methodologies/ax-template` → `claude plugin install ax-transform@ax-transform`,
**설치 후 새 세션부터** 로드됨).

## [1] 프로젝트 파악 — 조사 먼저, 질문은 그 다음

디렉토리 트리·package.json·build.gradle(.kts)·tsconfig 등을 직접 읽고 파악한다:

- 어떤 스택인가 (react / java / 둘 다 / 그 외)
- 소스가 어디 있고 디렉토리 구조가 어떤 모양인가
- 빈 디렉토리인가

그리고 **조사로 알 수 없는 것만** 사용자에게 묻는다 — 최소한 이것은 반드시 확인:

- 이 프로젝트의 목적/성격 (한 줄)
- 카탈로그를 어느 범위에 적용할지 (전체 / 특정 하위 디렉토리)
- 기계 강제([4])까지 원하는지, 원하면 어떤 것(react lint / java test / 훅)

빈 디렉토리라면: 어떤 스택으로 시작할지 물은 뒤, **그 스택의 공식 스캐폴드**로 뼈대를
만들고(react: `npx create-next-app@latest .` / java: start.spring.io) git init + 첫 커밋
후 진행한다. ax-template에서 가져오지 않는다.

성공 기준: 스택·구조·적용 범위가 확정되고 사용자 답을 받았다.

## [2] 프로젝트 선언 — `/ax-transform:ax-init-config`

성공 기준: 감지 결과가 담긴 `ax.config.json` **초안이 먼저 제시되고**, 사용자가 승인한
뒤에만 파일이 생성된다. 승인 없이 파일이 생기면 그 자체가 결함 → [F] 기록.

★ 초안을 보여줄 때, 아래 4가지를 [1]에서 조사한 실제 구조와 하나씩 대조한 표를 함께
제시하라. 여기가 틀리면 이후 lint가 **조용히 0위반**이 된다 — 이 채널의 대표 실패 모드다.

| 필드 | 대조 기준 |
|---|---|
| `react.root` / `java.root` | 실제 코드 위치인가 (프로젝트 루트면 `"."`) |
| `srcDir` | 실제 소스 디렉토리명인가 — **단일 세그먼트만**. `packages/web/src` 같은 다중 세그먼트는 런타임 throw |
| `alias` | tsconfig paths / vite alias 값과 일치하는가 |
| `layers` | app/features/shared 각각의 **디렉토리명 배열**이 실제 구조와 맞는가 (3계층 자체는 불변식) |

불일치는 고친 초안으로 다시 승인받는다.

## [3] 지식 레이어 동작 확인 — `/ax-transform:ax-practices`

실제로 손댈 파일 1~2개를 대상으로 실행한다.

성공 기준: 지적마다 룰 id가 인용된다 (`ax/<id>` 또는 `practices/rules/<id>.md`).
인용 없는 지적은 카탈로그 근거가 없으므로 채택하지 않는다.
이 단계에서 gradle/eslint 기계 검증을 대행했다고 주장하면 결함이다 — "미설치" 안내가
정상이고, 실제 차단은 [4]에서만 생긴다.

## [4] 기계 강제 배선 — [1]에서 사용자가 원한다고 답한 것만

| 스킬 | 필요 도구 |
|---|---|
| `/ax-transform:ax-install-react-enforcement` | node/npm + ESLint 9 |
| `/ax-transform:ax-install-java-enforcement` | JDK + gradle |
| `/ax-transform:ax-install-hooks` | git |

필요 도구가 없으면 설치를 임의로 하지 말고 사용자에게 물어라.

각 스킬이 배선 직후 probe(위반 심기 → 차단/검출 확인 → 삭제)를 스스로 수행한다.
★ 관측물이 스택마다 다르다 — 엉뚱한 것을 찾지 마라:

- react — lint 출력에 **룰 id**가 뜬다 (예: `ax/no-upward-layer-import`)
- java — 레이어 경계 테스트가 **RED로 실패**, 출력에 **probe 클래스명** (룰 id 아님)
- hooks — `git commit`이 **실제로 거부**된다

probe에서 아무것도 검출되지 않으면 배선이 공허한 것 — 멈추고 [F] 기록 후 보고.

⚠️ ax-template 레포의 `.githooks/`를 복사해 오지 마라 — 그 pre-push recency guard는
그 레포 전용이라 이 프로젝트의 **모든 push를 영구 차단**한다. 훅은 위 스킬로만.

## [F] 마찰 기록 (상시 — 이 세션의 두 번째 산출물)

막힌 지점 · 스킬 설명과 실제의 불일치 · 오해를 부른 표현을 발견하는 즉시 프로젝트
루트 `ax-feedback.md`에 append. 사소해 보여도 적는다.

```markdown
## <YYYY-MM-DD> [<단계번호>] <한 줄 요약>
- 기대: <스킬이 이렇게 될 거라 했다 — 스킬명·문구 인용>
- 실제: <실제로 이렇게 됐다 — 출력 원문>
- 영향: <막힘 / 우회함 / 오해만 하고 지나감>
- 추정 원인: <모르면 "미상". 추측을 사실처럼 쓰지 마라>
```

## [완료 보고]

마지막에 한 블록으로 출력한다 (사용자가 그대로 복사해 카탈로그 쪽에 전달한다):

1. 생성된 `ax.config.json` 전문
2. 배선한 강제 목록 + 각 probe의 **관측 증거**(출력 한두 줄)
3. `ax-feedback.md` 전문 (없으면 "마찰 없음")
4. 이 프로젝트에서 다음에 할 일 제안

---

**시작 순서**: [0]~[1]을 먼저 수행하라 — 조사 결과와 질문을 정리해 사용자에게 확인받은
뒤 [2]로 간다. 승인 없이 [3] 이후로 넘어가지 마라.

**이 문서의 범위**: 구성(setup)까지다 — 기능 개발은 하지 않는다. 구성 완료 후의 개발은
사용자의 PRD/spec을 받아 별도로 진행하며, 그때 `/ax-transform:ax-practices`를 코딩·리뷰에
상시 적용한다.
