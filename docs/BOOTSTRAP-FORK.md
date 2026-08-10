# BOOTSTRAP-FORK — fork-as-base(경로 A) 자동 구성 지시문

> **이 문서는 AI 에이전트(Claude Code 세션)가 읽고 그대로 실행하는 지시문이다.**
> 사람에게 주는 설명문은 [GETTING-STARTED.md](GETTING-STARTED.md)·[PLUGIN-CHANNEL.md](PLUGIN-CHANNEL.md)가 정본.
> 전달용 짧은 프롬프트는 [START-PROMPTS.md](START-PROMPTS.md) 참조.

---

너는 ax-template을 fork-base로 삼아 새 프로젝트를 시작한다.
**시작 전에 사용자에게 물어라**: ① 프로젝트 이름 ② 목표 도메인(한 줄 — 예: 구독 결제가
있는 B2B 관리자 툴).

아래를 순서대로 실행하고, 각 단계는 **"성공 기준"이 관측될 때만** 다음으로 넘어간다.
기준이 안 나오면 멈추고 무엇이 나왔는지 그대로 보고한다. 추정으로 진행하지 마라.

**소요(실측)**: 빌드까지 ~15분, [9] 최종 판정까지 완주하면 1시간 안팎.

## [0] 도구 준비 (신규 PC이거나 도구가 의심되면 — 없는 것만 설치)

```bash
brew --version                          # 없으면 https://brew.sh 원라이너
java -version                           # 21 아니면: brew install openjdk@21
python3 -c 'import yaml'                # 실패 시: pip3 install pyyaml
node -v && npm -v                       # 없으면: brew install node
git --version                           # ≥ 2.28 필요
```

성공 기준: 다섯 검사 전부 통과 출력.

JDK 주의 — macOS `/usr/bin/java`는 껍데기다. Homebrew JDK는 `java_home`에 등록되지
않으므로 `$(/usr/libexec/java_home -v 21)`은 실패한다(실측). 직접 경로가 정답:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
# Intel Mac: /usr/local/opt/...  · system/Oracle JDK만 $(/usr/libexec/java_home -v 21) 가능
```

PyYAML이 없고 yq만 있으면 백엔드 전용 실행은 되지만 [9] 전체 실행은 막힌다.

## [1] 클론

```bash
git clone https://github.com/ai-dev-methodologies/ax-template <PROJECT_NAME>
cd <PROJECT_NAME> && git submodule update --init
```

성공 기준: 클론 완료. submodule 3개(petclinic/realworld/modulith)는 **선택** —
이식성 축 검증에만 쓰이고, 가드·게이트·빌드는 서브모듈 없이도 통과한다.
외부 서드파티 repo 접근이 막혀 있으면 서브모듈 실패는 무시하고 [2]로 진행한다.

## [2] 사전 요구사항 재확인 — 없으면 이후 검증이 exit 2로 막힌다

`java -version`(21) · `python3 -V` · `python3 -c 'import yaml'` · `node -v` · `git --version`

성공 기준: 전부 출력. JDK가 21이 아니면 [0]의 JAVA_HOME부터 잡는다.

## [3] 현재 상태 파악 — 건너뛰면 범위를 오판한다

`docs/IMPLEMENTATION-STATUS.md`를 읽고, 25개 L4 도메인을
full-trio(백엔드+프론트 트리오) / backend-only(서버간 도메인, 프론트 의도적 부재) /
rules-as-code(INFRA)로 분류해 요약 보고한다.
※ "spec만 있는 도메인"은 없다 — 25개 전부 백엔드 구현을 갖는다.

성공 기준: 사용자의 목표 도메인이 위 셋 중 무엇이며, 프론트가 있는지 없는지 명시됨.

## [4] 레시피 선택 (선택이지만 권장)

`recipes/_MANIFEST.yaml`에서 11개 레시피 중 목표에 가장 가까운 것을 고르고
`recipes/<이름>/RECIPE.md`의 "Backend Implementation Status" 표를 읽어
바로 쓸 수 있는 것 vs 구현해야 하는 것을 표로 보고한다.

## [5] 강제 훅 활성화 — 이걸 해야 커밋/푸시 게이트가 켜진다 (opt-in)

```bash
bash practices/scripts/install-hooks.sh
```

성공 기준: `git config core.hooksPath`가 `.githooks`를 가리킴.

## [6] 빌드

```bash
cd backend && ./gradlew build && cd ..
cd frontend && npm ci && cd ..
```

성공 기준: gradle이 `BUILD SUCCESSFUL`, npm이 `added N packages ... in Xs`.
※ 가장 오래 걸리는 단계 — 백엔드 빌드는 테스트 포함 10분 안팎(부하에 따라 더).
멈춘 게 아니다.

## [7] 카탈로그 무결성 확인

```bash
bash practices/evals/run-all-guards.sh
```

성공 기준: 마지막 두 줄이 `Total: N passed, 0 failed` + `run-all-guards: all guards PASS`.
FAIL이 있으면 그 이름과 출력을 그대로 보고하고 멈춘다. ※ 10분 이상 걸린다(실측).

## [8] ★강제가 정말 막는지 증명 — 자기보고 금지, 실제 차단을 본다

```bash
bash practices/scripts/ax-prove-gate-blocks-agent.sh
bash practices/scripts/ax-prove-evidence-gate-blocks-agent.sh
```

성공 기준: 두 스크립트 모두 "위반 심음 → 차단됨 → 고침 → 통과" 3단계를 출력.
차단이 관측되지 않으면 강제가 꺼진 것이다 — 멈추고 보고.

## [9] 최종 판정

```bash
bash practices/scripts/verify-completion.sh
```

성공 기준: exit 0. exit 1이면 출력된 fix_playbook을 적용하고 재실행.
exit 2는 도구 누락 — [0]/[2]로 돌아간다.

## [10] 이후 개발 규율 (이 프로젝트의 상시 규칙)

- 새 도메인은 코드가 아니라 spec부터 — `METHODOLOGY.md` 5단계 +
  `docs/NEW-DOMAIN-CHECKLIST.md`의 필수 산출물(entity·repo·service·thin controller·
  state machine·domain advice·V###.sql·ComplianceTest·ViolationProofTest·per-domain
  gradle task)을 빠짐없이 만든다.
- 새 규칙은 evidence 없이 못 만든다 — 외부 URL/인용이 있어야 게이트를 통과한다.
- "완료"는 `verify-completion.sh` exit 0으로만 선언한다.

---

**시작 순서**: 프로젝트 이름·목표 도메인을 사용자에게 확인한 뒤 [0]~[3]을 실행하고
결과를 보고해 진행 승인을 받아라.

**켜지는 강제 요약**: 커밋 시 4 하드게이트(practices/ 변경) · 푸시 시 R25 recency + 회귀 ·
완료 선언 전 R25 전체. CI 통합은 fork 팀 자율(템플릿이 강제하지 않음).
