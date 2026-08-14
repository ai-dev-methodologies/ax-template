# 사용설명서 — ax-template 실사용 운영 매뉴얼

> 이 문서는 **운영 절차서**다: 설치 전제 조건, 머신/계정 셋업, 확인 방법, 업데이트, 트러블슈팅.
> 어느 소비 경로를 고를지·각 스킬이 무엇을 하는지의 **개념 설명은 [PLUGIN-CHANNEL.md](PLUGIN-CHANNEL.md)가 정본**이다.
> fork-as-base(경로 A) 시작 절차는 [GETTING-STARTED.md](GETTING-STARTED.md) · README의 30분 quickstart 참조.

## 문서 지도

| 알고 싶은 것 | 문서 |
|---|---|
| 두 소비 경로(A fork / B plugin) 비교·선택 | [PLUGIN-CHANNEL.md](PLUGIN-CHANNEL.md) |
| 경로별 복붙용 시작 프롬프트 | [START-PROMPTS.md](START-PROMPTS.md) |
| plugin 채널 설치·프로젝트 온보딩·트러블슈팅 (이 문서) | USAGE-GUIDE.md |
| fork-as-base로 새 도메인 추가 | [NEW-DOMAIN-CHECKLIST.md](NEW-DOMAIN-CHECKLIST.md) + METHODOLOGY.md |
| 결정 이력 (왜 이렇게 설계됐나) | practices/DECISIONS.md (plugin 채널은 R109) |

---

## 1. 설치 전제 조건 (머신/계정당)

| 요구사항 | 확인 명령 | 비고 |
|---|---|---|
| Claude Code CLI | `claude --version` | plugin 명령은 CLI 또는 세션 내 `/plugin` 둘 다 가능 |
| 네트워크 접근 | `git ls-remote https://github.com/ai-dev-methodologies/ax-template HEAD` | public repo — 인증 없이 HTTPS로 동작한다(실측: `gh repo view … --json visibility` → `PUBLIC`). 이 명령이 sha를 출력해야 설치 가능. 예외: git이 `url.*.insteadOf`로 https→ssh 재작성되도록 설정돼 있으면 그 계정의 SSH 키가 대신 필요 — T-3 참조 |
| git ≥ 2.28 | `git --version` | ⚠️ 아래 트러블슈팅 T-4 — PATH 앞에 낡은 git이 숨어 있는 머신 실존 |

plugin 채널 소비만 할 거면 JDK/node는 **필요 없다** (기계 강제를 opt-in 설치할 때 해당 스택 도구만 필요).

## 2. 설치 (머신의 계정당 1회)

```bash
claude plugin marketplace add ai-dev-methodologies/ax-template
claude plugin install ax-transform@ax-transform
```

(Claude Code 세션 안에서는 `/plugin marketplace add ai-dev-methodologies/ax-template` → `/plugin install ax-transform@ax-transform` 동일.)

**설치 확인:**

```bash
claude plugin list          # ax-transform@ax-transform · Status: ✔ enabled 이어야 함
```

- scope는 `user` — **이 계정의 모든 프로젝트에 적용**된다.
- 스킬 26종이 로드된다. **진행 중이던 세션에는 반영되지 않는다 — 새 세션부터.**
- 플러그인으로 설치된 스킬은 `ax-transform:` 네임스페이스로 등록된다
  (예: `/ax-transform:ax-practices`). 짧은 이름(`/ax-practices`)을 입력해도 대부분
  자동 매칭되지만, 동명 스킬이 있는 환경에서는 풀네임을 쓰라.
- 레포 루트의 `skills/`는 Claude Code가 직접 로드하는 경로가 아니다 — ax-template 레포
  안에서 여는 세션도 스킬은 **설치된 플러그인을 통해** (동일 네임스페이스로) 쓴다.

## 3. 프로젝트 온보딩 (프로젝트당 1회)

대상 프로젝트 루트에서 새 세션을 열고:

```
/ax-init-config
```

스택(package.json / build.gradle)·디렉토리 구조를 감지해 `ax.config.json` 초안을 **보여주고,
승인해야만** 파일을 만든다 (자동 생성하지 않는다). 필드 레퍼런스:

```jsonc
{
  "version": 1,
  "stacks": ["react", "java"],        // 배열 — 쓰는 스택만. ["react"]만도 됨
  "react": {
    "root": "frontend",               // 프로젝트 루트 기준 react 코드 위치. 루트면 "."
    "srcDir": "src",                  // 단일 세그먼트만 — 스키마(^[^/]+$) + 런타임 throw 이중 강제
    "alias": { "@/": "src/" },        // import alias → 실경로 매핑
    "layers": {                       // 레이어 3계층(app>features>shared)은 불변 —
      "app": ["app"],                 // 각 레이어의 "디렉토리명 배열"만 커스텀
      "features": ["features"],
      "shared": ["components", "lib"]
    }
  },
  "java": { "root": "backend", "buildTool": "gradle", "rootPackage": "com.mycompany.app" }
}
```

- 스키마 정본: `practices-react/eslint-plugin-ax/schemas/ax.config.schema.json`
- 샘플: 레포 루트 `ax.config.sample.json`

## 4. 일상 사용

코딩/리뷰 중:

```
/ax-practices
```

동작 요약 (상세는 PLUGIN-CHANNEL.md 3단계): config 읽기 → 해당 스택 INDEX만 로드 →
작업 맥락 관련 룰 **최대 8개**만 본문 로드 → review형 룰 적용 + 룰 id 인용.
기계 검증(gradle/eslint)은 설치 전이면 "미설치" 안내만 한다 — 대행 주장하지 않는다.

## 5. 기계 강제 배선 (원할 때만, opt-in)

| 스킬 | 배선 대상 | 필요 도구 |
|---|---|---|
| `/ax-install-react-enforcement` | ESLint 플러그인 `file:` 설치 + `settings.ax` | node/npm, ESLint 9 |
| `/ax-install-java-enforcement` | ArchUnit 시작 체크 3종 + `testPractices` task | JDK, gradle |
| `/ax-install-hooks` | pre-commit 훅 | git |

**세 설치 스킬 모두** 배선 직후 **probe 검증**(위반 심기 → `git commit`/lint/test 시도 →
차단 확인 → 삭제)까지 자동 수행한다 — "설치했는데 조용히 아무것도 안 잡는" 상태를 그
자리에서 걸러낸다. 다만 각 probe가 증명하는 범위는 스킬마다 다르다: react/java probe는
"그 스택의 특정 룰이 실제로 검출되는가"까지 증명하고(rule id / RED 테스트 클래스명이
출력에 뜬다), hooks probe는 "git이 훅을 실제로 실행해 커밋을 막는가"까지 증명한다 —
스택별 게이트가 이미 설치돼 있으면 그 스킬의 probe 파일을 그대로 재사용해 전체 체인
(`git commit` → 훅 → `npm run lint`/`./gradlew testPractices` → 카탈로그 룰)을 검증하고,
아직 아무 게이트도 없으면 훅 파일에 임시 `exit 1`을 심어 배선 자체(hooksPath/husky/
lefthook → 실제 커밋 차단)만 검증한다.

> **헤드리스 실행 주의**: 이 절의 설치 스킬(특히 `/ax-install-react-enforcement`)은 `npm install`과
> probe 실행에 Bash가 필요하다. `claude -p` 등 헤드리스 실행에서 `--permission-mode acceptEdits`는
> 파일 편집만 자동 승인하고 Bash 호출은 여전히 승인 대기시키므로 npm 설치·probe가 막힌다. 헤드리스
> 운영자는 `--permission-mode bypassPermissions`(신뢰된 샌드박스 한정) 또는 `.claude/settings.json`의
> `permissions.allow`에 필요한 Bash 패턴(`npm i *`, `npx eslint *`)을 사전 등록해야 한다. 대화형
> 세션은 승인 프롬프트가 그때그때 뜨므로 영향 없다.

## 6. 업데이트 / 제거

세션이 로드하는 것은 `~/.claude/plugins/cache/`의 **설치 시점 스냅숏**이다 — marketplace
갱신만으로는 반영되지 않는다.

**정상 경로 (BACKLOG D-7 종결 — 2026-08-10부터, live 실증됨):**

```bash
claude plugin marketplace update ax-transform           # ① 카탈로그 clone 갱신
claude plugin update ax-transform@ax-transform          # ② plugin.json 버전이 올랐으면 새 스냅숏 설치
claude plugin list                                      # 확인 — 적용은 새 세션부터
```

`claude plugin update`는 `.claude-plugin/plugin.json`의 **top-level `version` 필드만** 보고
갱신 여부를 판단한다(2026-08-02 live 실측, CLI 2.1.220) — `marketplace.json`의 plugin-entry
`version`이나 `metadata.version`은 이 비교에 쓰이지 않는다. ax-template은 이제 **내용이 바뀌는
릴리스마다 `plugin.json`의 version을 올리는 규율**을 채택했고(`practices/DECISIONS.md` R111),
`doc_headline_count_guard.sh`가 `plugin.json`과 `marketplace.json`의 세 버전 필드(엔트리
version · `metadata.version` · `plugin.json` version)가 서로 어긋나지 않는지를 기계적으로
검증한다 — 한쪽만 올리고 잊는 릴리스를 차단한다.

**이 경로가 no-op으로 보일 수 있는 경우 (여전히 유효한 사실 — amended, not retracted):**
plugin.json의 버전 필드가 실제로 올라가지 **않은** 채 내용만 바뀐 릴리스를 소비 중이라면(예:
D-7 이전에 설치했거나, 릴리스 규율을 어긴 커밋을 소비 중인 경우) `claude plugin update`는
"already at the latest version"을 출력하는 **진짜 no-op**이다 — 스냅숏의 version 문자열이
plugin.json과 이미 같기 때문이며, 업데이터에 결함이 있는 게 아니다(2026-08-02 최초 실측:
plugin.json이 0.1.0에 고정돼 있던 시절 gitCommitSha 불변 확인). 이 경우의 **확실한 우회 경로**:

```bash
claude plugin uninstall ax-transform@ax-transform       # ① 스냅숏 제거
claude plugin install ax-transform@ax-transform         # ② 최신 main으로 재설치
claude plugin list                                      # 확인 — 적용은 새 세션부터
```

> ⚠️ **갱신 후 CLI 프로세스를 완전히 재시작하라 — `/clear`로는 부족하다** (GH #88, 실측).
> 스킬 레지스트리는 **프로세스 시작 시점의 스냅숏**이라, 실행 중인 세션은 갱신 전 버전을 계속
> 서빙한다. 반면 `claude plugin list`는 별도 서브프로세스로 **디스크 상태**를 읽으므로 새 버전을
> 보고한다 — 즉 세션이 자연스럽게 확인할 그 신호가 실제 로드된 것과 어긋난다. 실측 사례: `plugin
> list`가 0.1.5를 보고하는 동안 Skill 툴은 0.1.3을 서빙했고, 그대로 검증했다면 이미 고쳐진 결함을
> "여전히 깨짐"으로 **거짓 보고**할 뻔했다.
>
> **세션이 실제로 실행 중인 버전을 확인하는 유일한 신뢰 신호**: Skill 호출 출력의
> `Base directory for this skill:` 경로에 박힌 버전 번호. 릴리스 검증은 반드시 **새로 띄운 CLI
> 프로세스**에서 하라. (근본 원인은 Claude Code 하네스 동작이며 ax-template이 강제할 수 없다.)

**갱신 후 — react 기계 강제를 배선한 프로젝트는 심링크 재결박 (경로에 버전이 박혀 있다):**
플러그인 스냅숏 경로는 `…/cache/ax-transform/ax-transform/<plugin버전>/` 형태라, **버전이
올라간 갱신 뒤에는** 소비 프로젝트의 `node_modules/@ax/eslint-plugin-ax` 심링크가 **옛 버전
디렉토리를 계속 가리킨다**(2026-08-10 git-소스 실측: same-버전 재설치는 같은 경로를 재생성해
심링크가 생존하지만, 버전이 바뀌면 경로 자체가 바뀐다). 갱신 후 각 소비 프로젝트에서
`npm i -D file:<새 스냅숏 경로>/practices-react/eslint-plugin-ax`를 재실행해 재결박하고,
`/ax-install-react-enforcement`의 probe 절차로 배선이 살아 있는지 재확인한다.

제거만 할 때:

```bash
claude plugin uninstall ax-transform@ax-transform
claude plugin marketplace remove ax-transform
```

## 6b. 이미 설치된 프로젝트 직접 패치 (0.1.6 → 0.1.7 업데이트 없이)

`/ax-install-java-enforcement`로 **0.1.6** 스냅숏을 이미 설치한 프로젝트는 §6의 갱신 경로(재설치)를
밟지 않고도 아래 3개 BLOCKING 결함을 손으로 고칠 수 있다. 셋 다 GH 이슈로 보고됐고 이미
`skills/ax-install-java-enforcement/SKILL.md`에서 봉합됐다 — **아래 패치의 최종 형태는 그 파일의
현재 내용과 정확히 일치해야 한다.** 이 절과 그 SKILL.md가 어긋나면 SKILL.md가 정본이다.

- [GH #89](https://github.com/ai-dev-methodologies/ax-template/issues/89) — `compileTestJava` 실패
- [GH #90](https://github.com/ai-dev-methodologies/ax-template/issues/90) — `-P` 없는 모든 gradle 호출 실패
- [GH #91](https://github.com/ai-dev-methodologies/ax-template/issues/91) — `-P`를 줘도 `test`/`build`/CI 실패

세 결함 모두 프로젝트의 `<java.root>/build.gradle.kts`와
`<java.root>/src/test/java/<rootPackage>/archunit/LayerBoundaryArchTest.java`(§3 4단계에서 생성된
파일 — 프로젝트마다 `rootPackage` 경로만 다르다)를 손으로 편집해 고친다.

### #89 — `compileTestJava` 실패

**증상**: `./gradlew compileTestJava`가 `method that in class JavaClasses cannot be applied to
given types` 로 실패.

**원인**: `LayerBoundaryArchTest`가 이미 `private static JavaClasses classes()` 헬퍼를 갖고 있는데,
`ArchRuleDefinition.classes`를 **static import**하면 같은 simple name 충돌에서 멤버 메서드가
이긴다 — DTO 룰의 비한정 `classes().that()...beRecords()` 호출이 (ArchUnit의 팩토리가 아니라)
그 헬퍼에 바인딩돼 시그니처가 안 맞는다.

**패치** (`LayerBoundaryArchTest.java`):
1. `import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;` 줄을 **제거**한다
   (`noClasses`의 static import는 유지 — 이름 충돌이 없어 그대로 안전하다).
2. 비한정 `import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;` (non-static)이 없다면
   추가한다.
3. `requestAndResponseClassesAreRecords()` 안의 호출을 한정 호출로 바꾼다:
   ```java
   // before
   ArchRule rule = classes()
           .that().haveSimpleNameEndingWith("Request")
   // after
   ArchRule rule = ArchRuleDefinition.classes()
           .that().haveSimpleNameEndingWith("Request")
   ```
   (`.or().haveSimpleNameEndingWith("Response").should().beRecords().allowEmptyShould(true);`
   이하는 변경 없음.)

### #90 — `-P` 없는 **모든** gradle 호출 실패

**증상**: `-PaxRootPackage`를 주지 않은 임의의 gradle 호출이 전부 실패한다 — `./gradlew tasks`
조차 `Could not create task ':testPractices'`로 죽는다.

**원인**: `?: error("axRootPackage unresolved...")`가 `testPractices` task **등록 블록 안,
configuration 시점**에 있었다. 스톡 Spring Initializr 스캐폴드가 이미 eager한
`tasks.withType<Test> { useJUnitPlatform() }`를 갖고 있어 이 task를 포함한 모든 `Test` task가
configuration 단계에서 realize되고, 그 순간 `-P`가 없으면 즉시 예외가 던져져 build 전체가
죽는다 — 게이트 하나가 무관한 다른 모든 task까지 인질로 잡는 셈이다.

**패치** (`build.gradle.kts`): 그 `?: error(...)` 라인(과 그것이 계산하는
`systemProperty("ax.rootPackage", rootPackage)` / `systemProperty("ax.mainClassesDirs", ...)`
호출)을 task 등록 몸체 최상위에서 꺼내 **`doFirst { }` 블록 안으로 옮긴다** — configuration
시점이 아니라 **execution 시점**에 평가되게 한다:
   ```kotlin
   tasks.register<Test>("testPractices") {
       group = "verification"
       description = "Runs the ax practices ArchUnit gate (JUnit tag PRACTICES)."
       testClassesDirs = sourceSets["test"].output.classesDirs
       classpath = sourceSets["test"].runtimeClasspath
       useJUnitPlatform { includeTags("PRACTICES") }
       testLogging {
           exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
       }

       // execution time, not configuration time — an unresolved axRootPackage must abort
       // THIS task, not every task in the build.
       doFirst {
           val rootPackage = axRootPackage.orNull
               ?: error("axRootPackage unresolved -- pass -PaxRootPackage=<your root package>")
           systemProperty("ax.rootPackage", rootPackage)
           systemProperty(
               "ax.mainClassesDirs",
               sourceSets["main"].output.classesDirs.files.joinToString(File.pathSeparator) { it.absolutePath }
           )
       }
   }
   ```
   `-P` 없이 던지던 예외가 `doFirst`로 옮겨진 뒤에도 **`testPractices`를 직접 돌리면 여전히
   fail-closed로 죽는다** — 달라지는 것은 그 실패가 `testPractices` 자신의 실행에만 국한되고,
   `./gradlew tasks`/`build`/무관한 다른 task는 더 이상 물귀신으로 끌려가지 않는다는 점이다.

### #91 — `-P`를 줘도 `test`/`build`/CI가 실패

**증상**: `-PaxRootPackage`를 정확히 줘도 `./gradlew test`, `./gradlew build`, CI 파이프라인이
실패한다.

**원인**: 기본 `test` task가 `@Tag("PRACTICES")`가 붙은 ArchUnit 클래스까지 흡수해서 실행하는데,
`test` task 실행 경로에는 `testPractices`만 채우는 `ax.mainClassesDirs` 시스템 프로퍼티가 없어
그 클래스들이 거기서 죽는다.

**패치** (`build.gradle.kts`): 기본 `test` task에서 `PRACTICES` 태그를 명시적으로 제외하는 블록을
추가한다:
   ```kotlin
   tasks.named<Test>("test") {
       useJUnitPlatform { excludeTags("PRACTICES") }
   }
   ```

   ⚠️ **반드시 `tasks.named<Test>("test") { ... }`로 `test` task 하나만 한정**해야 한다. 이미
   프로젝트에 공유 `tasks.withType<Test>` 블록이 있다고 해서 그 안에 `excludeTags("PRACTICES")`를
   합치면, **`testPractices` 자신에게까지 그 exclude가 적용**되어 항상 0개 테스트로
   `BUILD SUCCESSFUL`을 내는 **무증상 공허(vacuous) GREEN**이 된다 — 정확히 GH #86~#88이 봉합한
   실패 형태다. `test`와 `testPractices`의 태그 필터는 서로 반대 방향이므로 절대 같은
   `tasks.withType<Test>` 블록에 두지 않는다.

### 패치 후 재검증 (건너뛰지 말 것)

세 패치를 손으로 적용한 뒤에는 `skills/ax-install-java-enforcement/SKILL.md` §5의 non-vacuous
verification 절차(0a `compileTestJava` → 0b `-P` 없는 `./gradlew tasks` → 0c
`./gradlew test -PaxRootPackage=...` → probe 위반 심기 RED → 삭제 GREEN)를 **처음부터 다시**
돌려 배선이 실제로 살아있는지 확인한다. 패치만 적용하고 재검증을 생략하면, 겉보기엔 고쳐진
것 같아도 §5가 잡는 정확히 그 종류의 "green이지만 실제로는 아무것도 안 잡는" 상태로 되돌아갈
수 있다.

---

## 7. 트러블슈팅

### T-1. 설치했는데 스킬이 안 뜬다
- 설치 **이후에 연 새 세션**인지 확인 (진행 중 세션에는 로드 안 됨).
- `claude plugin list`에서 Status가 `enabled`인지.
- 짧은 이름 매칭 실패 가능성 → 풀네임 `/ax-transform:ax-practices`로 시도.

### T-2. lint가 0위반인데 실제 위반이 있다 (silent-miss)
plugin 채널의 대표 함정. 다음 순서로 진단:
1. `eslint.config.mjs`의 `files` 글롭이 **실제 srcDir을 포함**하는가 — `src/**` 하드코딩
   상태에서 srcDir이 `source`면 lint 대상 자체가 0이다. `${axConfig.react.srcDir}/**` 형태여야 함.
2. `settings: { ax: axConfig.react }` 주입이 있는가 — 없으면 기본 레이아웃(`src/…`)으로
   폴백해 커스텀 트리 전체가 레이어 판정 밖.
3. `ax.config.json`의 `srcDir`가 다중 세그먼트(`packages/web/src` 류)면 — 이제 스키마뿐
   아니라 `layoutFrom()` 런타임도 이를 즉시 `Error` throw로 거부해 ESLint가 fatal error로
   죽는다(exit code 2). 즉 이 경우는 더 이상 "조용한 0위반"이 아니라 눈에 보이는 크래시로
   나타난다 — 크래시 메시지를 그대로 따라 단일 세그먼트로 고친다.
4. `/ax-install-react-enforcement`의 probe 절차를 다시 실행해 배선을 재검증.

### T-3. `marketplace add`가 실패한다
ax-template은 **public repo**다 — SSH 키/토큰은 정상 경로에 필요 없다. 실패 원인은 아래 순서로 좁힌다:

1. `owner/repo` 오타: `claude plugin marketplace add ai-dev-methodologies/ax-template` 인지 확인
   (하이픈·오탈자 한 글자 차이로도 조용히 실패).
2. 낡은 git이 PATH 앞에 있음: T-4의 `git init -b` 미지원 케이스와 같은 원인 —
   `which -a git`으로 실제 실행되는 git이 ≥ 2.28인지 확인.
3. `git config --get-all url.*.insteadOf` / `~/.gitconfig`에 https→ssh 재작성 규칙이
   있는지 확인 (`url."git@github.com:".insteadOf = "https://github.com/"` 류). 있으면
   `claude`가 내부적으로 https URL을 만들어도 실행 시 ssh로 바뀌어 그 계정의 SSH 키가
   없으면 실패한다 — 규칙을 제거하거나 키를 등록.
4. 네트워크/프록시: `git ls-remote https://github.com/ai-dev-methodologies/ax-template HEAD`가
   sha 없이 실패하면(타임아웃, 407, TLS 오류) 사내 프록시/방화벽이 github.com 자체를 막고 있는 것.
5. marketplace 이름 충돌: 이미 다른 소스로 `ax-transform`이라는 이름의 marketplace가 등록돼
   있으면 `add`가 거부될 수 있다 — `claude plugin marketplace list`로 기존 등록을 확인 후
   `claude plugin marketplace remove ax-transform`로 정리하고 재시도.

### T-4. (maintainer, 경로 A) R25 `verify-completion.sh`가 환경 문제로 BLOCK
전부 실측된 사례들이다. R25는 fail-closed라 **원인을 없애야** 하며 우회 옵션은 없다:

| 증상 | 원인 | 조치 |
|---|---|---|
| `RATCHET_TOOLCHAIN_MODIFIED` + filter.lfs 나열 | `~/.gitconfig`의 git-lfs content filter (ratchet 위협모델상 거부) | `git config --global --remove-section filter.lfs` 후 재실행. 다른 repo에서 LFS가 필요해지면 `git lfs install`로 복원 |
| `FINGERPRINT_UNVERIFIABLE` … GITLINK_DIVERGENCE | 서브모듈 작업트리에 미추적 잔여물 (예: portability 빌드가 만든 `mvnw`) | `git -C <서브모듈경로> clean -fd` 후 재실행 |
| `pre_push_decision_guard` 12건 전부 "scratch setup failed" | PATH 앞의 git이 2.28 미만 (`git init -b` 미지원 — /usr/local/bin에 2019년 git이 숨어있던 사례) | `which -a git`으로 확인, 낡은 git 제거 또는 `export PATH="/usr/bin:$PATH"` |
| preflight exit 2 (JDK) | JAVA_HOME 미지정/JDK 21 아님 | macOS Homebrew: `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` |
| frontend-lint 실패 | `frontend/node_modules` 부재 | `cd frontend && npm ci` (lockfile 커밋돼 있음) |
| recency/msi guard가 이유 없이 exit 2 | macOS 대소문자 aliased 경로에서 실행 | 물리 경로(디스크 스펠링 그대로)를 cwd로 실행 |

### T-5. `/ax-init-config`가 스택을 잘못 감지한다
감지는 제안일 뿐이다 — 초안을 승인하기 전에 수정 요청하거나, `ax.config.sample.json`을
복사해 직접 편집해도 된다. config는 평범한 JSON 파일이고 스킬보다 파일이 우선한다.

---

## 8. FAQ

**Q. 팀원/다른 계정에게 배포하려면?**
public repo이므로 접근 권한 부여가 필요 없다 — 그 계정에서 §2의 두 명령만 실행하면 끝.
머신 이동도 동일.

**Q. plugin 채널로 쓰면 ax-template의 guard·R25도 우리 프로젝트에 걸리나?**
아니다. 그것은 카탈로그 자신의 자기검증 체계다. 대상 프로젝트가 받는 것은
지식 레이어(/ax-practices) + opt-in으로 설치한 기계 강제(§5)뿐이다.

**Q. 카탈로그 룰을 우리 프로젝트 사정에 맞게 끄고 싶다.**
`ax.config.json`의 `rules.disabled`(룰 id 배열)·`rules.excludeTags`(태그 배열)를 쓴다.
ESLint 강제를 설치했다면 eslint config에서 해당 룰 off가 별도로 필요하다.

**Q. 21st.dev 파생 디자인 블록을 써도 되나?**
`templates/DERIVED-SOURCES.yaml`에 provenance가 등재된 채 패키지에 포함돼 있다
(upstream 라이선스 UNVERIFIED — 내부 사용 전제의 maintainer 결정, R109). **외부 공개
제품에 재배포할 계획이 생기면** 그 시점에 해당 대장 기준으로 라이선스 재심사가 필요하다.
