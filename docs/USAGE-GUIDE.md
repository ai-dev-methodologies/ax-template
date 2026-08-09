# 사용설명서 — ax-template 실사용 운영 매뉴얼

> 이 문서는 **운영 절차서**다: 설치 전제 조건, 머신/계정 셋업, 확인 방법, 업데이트, 트러블슈팅.
> 어느 소비 경로를 고를지·각 스킬이 무엇을 하는지의 **개념 설명은 [PLUGIN-CHANNEL.md](PLUGIN-CHANNEL.md)가 정본**이다.
> fork-as-base(경로 A) 시작 절차는 [GETTING-STARTED.md](GETTING-STARTED.md) · README의 30분 quickstart 참조.

## 문서 지도

| 알고 싶은 것 | 문서 |
|---|---|
| 두 소비 경로(A fork / B plugin) 비교·선택 | [PLUGIN-CHANNEL.md](PLUGIN-CHANNEL.md) |
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
    "srcDir": "src",                  // 단일 세그먼트만 (스키마 강제 ^[^/]+$)
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

**react·java 설치 스킬**은 설치 직후 **probe 검증**(위반 심기 → 검출 확인 → 삭제)까지
자동 수행한다 — "설치했는데 조용히 아무것도 안 잡는" 상태를 그 자리에서 걸러낸다.
hooks 스킬은 probe 없이 self-check 체크리스트로 마무리하므로, 훅 배선 후 위반 커밋을
한 번 시도해 차단을 직접 확인하는 것을 권장한다.

## 6. 업데이트 / 제거

세션이 로드하는 것은 `~/.claude/plugins/cache/`의 **설치 시점 스냅숏**이다 — marketplace
갱신만으로는 반영되지 않는다. 그리고 현 배포 모델에서는 `claude plugin update`도
**동작하지 않는다**: plugin.json 버전이 0.1.0에 고정돼 있어(레포 main = 배포 채널,
별도 릴리스 없음) 내용이 바뀌어도 updater가 "already at the latest version"으로
no-op한다 (2026-08-02 live 실측 — gitCommitSha 불변 확인).

**확실한 갱신 절차 (live 실증됨 — 스냅숏 sha가 최신 main으로 이동):**

```bash
claude plugin marketplace update ax-transform           # ① 카탈로그 clone 갱신
claude plugin uninstall ax-transform@ax-transform       # ② 스냅숏 제거
claude plugin install ax-transform@ax-transform         # ③ 최신 main으로 재설치
claude plugin list                                      # 확인 — 적용은 새 세션부터
```

> plugin.json 버전을 릴리스마다 올리는 규율이 도입되면(BACKLOG D-7) ②③은
> `claude plugin update ax-transform@ax-transform` 한 줄로 줄어든다. 그 전까지는
> 위 재설치 경로가 유일하게 검증된 방법이다.

제거만 할 때:

```bash
claude plugin uninstall ax-transform@ax-transform
claude plugin marketplace remove ax-transform
```

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
3. `ax.config.json`의 `srcDir`가 다중 세그먼트(`packages/web/src` 류)가 아닌가 — 스키마가
   막지만 손으로 편집했으면 통과됐을 수 있음. 단일 세그먼트로.
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
