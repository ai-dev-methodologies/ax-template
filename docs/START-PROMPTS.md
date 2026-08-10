# 시작 프롬프트 — 경로별 전달용 (짧은 프롬프트 + 문서 위치)

ax-template으로 프로젝트를 시작할 때, 대상 세션에 붙여넣는 것은 **아래 짧은 프롬프트
하나**다. 단계별 상세 지시는 main 브랜치의 부트스트랩 문서에 있고, 대상 세션이 그
문서를 직접 가져와 실행한다 — 프롬프트가 길어질수록 복사 실수·드리프트가 생기므로
**정본은 문서, 프롬프트는 포인터**로 유지한다.

- 어느 경로인지 아직 못 골랐으면 → [PLUGIN-CHANNEL.md](PLUGIN-CHANNEL.md)의 비교표가 정본.
- 두 부트스트랩 문서의 공통 설계: 각 단계에 **관측 가능한 성공 기준**, 마지막에
  **위반을 심어 차단을 증명**하는 단계. "설치했다"는 자기보고가 아니라 **차단 로그**가
  완료 증거다 (green ≠ correct).

---

## 경로 A — fork-as-base (신규 제품, 풀 강제)

**언제**: 새 제품을 처음부터 · 템플릿 구조(`backend/`+`frontend/`)를 물려받아도 됨 ·
가드/R25/훅 전부 원함. **상세 문서**: [BOOTSTRAP-FORK.md](BOOTSTRAP-FORK.md)

대상 세션에 붙여넣을 프롬프트:

```
https://raw.githubusercontent.com/ai-dev-methodologies/ax-template/main/docs/BOOTSTRAP-FORK.md
를 가져와(WebFetch 또는 curl) 전문을 읽고, 그 문서의 지시를 그대로 수행하라.
문서를 가져올 수 없으면 멈추고 보고하라. 문서와 다른 방식으로 임의 진행하지 마라.
```

## 경로 B — plugin 채널 (기존 프로젝트 / 자유 레이아웃 / fork 불원)

**언제**: 이미 있는 프로젝트 · 구조가 템플릿과 다름 · 카탈로그(지식)만 얹고 강제는
선택. **전제**: 이 계정에 플러그인이 설치돼 있어야 한다([USAGE-GUIDE.md](USAGE-GUIDE.md) §2,
계정당 1회). **상세 문서**: [BOOTSTRAP-SKILL.md](BOOTSTRAP-SKILL.md)

대상 세션에 붙여넣을 프롬프트:

```
https://raw.githubusercontent.com/ai-dev-methodologies/ax-template/main/docs/BOOTSTRAP-SKILL.md
를 가져와(WebFetch 또는 curl) 전문을 읽고, 그 문서의 지시를 그대로 수행하라.
문서를 가져올 수 없으면 멈추고 보고하라. 문서와 다른 방식으로 임의 진행하지 마라.
```

> 두 프롬프트 모두 **네트워크로 raw 문서를 가져온다** — public repo라 인증 불필요.
> 오프라인이거나 raw 접근이 막힌 환경이면 해당 문서 전문을 직접 붙여넣는 것으로 대체한다.

---

## 신규 PC 0단계 (Claude Code 설치 전 — 사람이 터미널에서 직접)

```bash
curl -fsSL https://claude.ai/install.sh | bash   # 또는 npm install -g @anthropic-ai/claude-code
git --version                                    # 없으면 xcode-select --install
claude                                           # 첫 실행 시 브라우저 인증
```

경로 A의 나머지 도구 준비(JDK 21·PyYAML·node)는 BOOTSTRAP-FORK.md의 [0]이 수행한다.
경로 B는 Claude Code + 네트워크만 있으면 된다.

## 두 경로를 섞어도 되나

된다. 흔한 조합은 **경로 A로 새 제품을 만들면서, 같은 계정의 다른 기존 프로젝트에는
경로 B를 얹는 것**이다. 플러그인 설치는 계정 단위(user scope)라 경로 A 레포 안에서
여는 세션도 스킬은 설치된 플러그인을 통해 쓴다(레포 루트의 `skills/`는 Claude Code가
직접 로드하는 경로가 아니다).

## 구성 이후의 개발

부트스트랩 문서의 범위는 **구성(setup)까지**다. 이후 개발은:

1. 무엇을 만들지 논의·PRD/spec — 사용자(팀) 몫, ax-template 밖.
2. 경로 A는 spec-first가 기계 강제된다(METHODOLOGY 5단계 + NEW-DOMAIN-CHECKLIST).
   경로 B는 spec을 사용자가 준비하고, 구현·리뷰에 `/ax-transform:ax-practices`를 상시
   적용한다 — 스킬은 spec을 대체하지 않고 그 위에 룰 준수를 얹는다.
