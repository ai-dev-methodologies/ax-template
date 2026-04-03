# TEMPLATE GOVERNANCE

작성일: 2026-04-02
프로젝트: ax-template
상태: CANONICAL
적용 범위: `ax-template`에서 사용하는 scaffold / architecture baseline / quality companion template 전부

## 1. 문서 목적
이 문서는 `ax-template`에서 템플릿 코드를 **어떻게 찾고, 어떤 기준으로 선택하고, 무엇을 canonical로 승격하며, 그 이후 어떻게 최신성과 안정성을 계속 확인할지**를 정의한다.

핵심 원칙은 단순하다.

> 외부 repo는 후보일 뿐이다.  
> 실제 자동화와 구현의 기준은 검증을 거친 **내부 canonical template** 뿐이다.

즉 우리는 star가 많은 repo를 매번 바로 템플릿으로 쓰지 않는다.
우리는 후보를 충분히 모은 뒤, reference implementation을 고르고, 그것을 검증/정규화/승격해서 내부 template truth로 사용한다.

---
## 2. Mental Model
템플릿은 boilerplate가 아니다.
템플릿은 **validated capability pack**이다.

즉 템플릿 하나는 아래 4층을 같이 가져야 한다.

1. **구조**
   - project shape
   - directory structure
   - module boundary
   - auth / config / data flow rules

2. **구현 기본값**
   - runtime
   - router
   - validation
   - logging
   - async/default deployment shape

3. **품질/검증 기준**
   - testing baseline
   - quality gates
   - verification checkpoints
   - anti-patterns
   - reject rules

4. **source provenance**
   - official docs
   - approved GitHub refs
   - practical refs
   - last reviewed timestamp

내부 근거:
- `docs/reference/BP-TEMPLATE-PACK-PROJECT.md`
- `docs/reference/BP-TEMPLATE-PACK-SCHEMA.md`
- `docs/reference/BP-TEMPLATE-PACK-CONSUMER-CONTRACT.md`
- `docs/reference/BP-TEMPLATE-PACK-ASSET-SHAPE.md`

---
## 3. Discovery와 Curation은 분리한다
### Discovery
후보를 넓게 모은다.

후보 소스:
1. 공식 문서
2. approved GitHub repo
3. practical reference repo
4. internal prior assets
5. engineering blog / medium / article corpus

이 단계에서 star 수는 **후보 발견용 신호**일 뿐이다.
canonical 선정 근거가 아니다.

### Curation
후보 중 하나를 reference implementation으로 고르거나,
여러 후보의 장점을 합쳐 internal canonical draft를 만든다.

즉 우리는 외부 repo를 그대로 소비하지 않는다.
외부 repo를 **검증 대상 후보**로 소비한다.

외부 관행 근거:
- mature platform teams는 discovery와 golden path curation을 분리한다
- internal best implementation을 hardened starter로 만들거나
- 여러 구현의 장점을 합쳐 internal canonical template로 만든다
- 외부 OSS starter는 그대로 링크하지 않고 fork / harden / wrap 한다

---
## 4. Template Candidate Selection Workflow
### Phase 1. Candidate Collection
각 template family에 대해 후보를 수집한다.

예:
- scaffold template
- architecture baseline template
- quality companion template

후보 수집 체크:
- 공식 문서 존재 여부
- 최근 유지보수 여부
- release cadence
- security posture
- testing/CI 존재 여부
- enterprise 운영 흔적

### Phase 2. Candidate Comparison
후보를 같은 표로 정규화해서 비교한다.

필수 비교 항목:
- What it is
- When to use
- Default structure
- Testing baseline
- Quality gates
- Anti-patterns
- Source provenance

비교 축:
1. 공식 문서 일치도
2. boring default 적합성
3. enterprise 운영성
4. 테스트/verify 가능성
5. 보안성
6. SI delivery 적합성
7. 유지보수 난이도

### Phase 3. Reference Selection
하나의 reference implementation을 선택한다.
또는 여러 후보의 장점을 합쳐 internal canonical draft를 만든다.

선정 기준:
- 가장 최신이라서가 아니라
- 가장 boring하고
- 가장 검증 가능하고
- 가장 운영 가능하고
- 가장 SI 현장에 맞는가

---
## 5. `chub`의 역할
`chub`는 freshness gate다. stable 판정자가 아니다.

### `chub`가 하는 일
- latest official docs 확인
- deprecated 여부 확인
- 최신 권장 API / file convention 확인
- mismatch 감지
- gotcha / workaround annotation

### `chub`가 하지 않는 일
- enterprise 운영 적합성 판정
- 템플릿 stable 판정
- boring default 여부 최종 판정
- 테스트/verify 통과 판정

### `chub` discipline
- draft completion 시 1회 mandatory check
- refresh 시 mandatory check
- unavailable / timeout / mismatch면 fail-open 금지

정책:
```yaml
freshness_policy:
  provider: chub
  required: true
  on_unavailable: stale
  on_timeout: stale
  on_no_match: review_required
  on_mismatch: stale
```

핵심 문장:
> `chub`는 최신성 검증 도구다.  
> 템플릿 채택 결정은 `chub` + 테스트 + verify + 실제 사용 검토를 함께 보고 내린다.

---
## 6. Status Model
`ax-template` 템플릿은 아래 상태를 가진다.

```text
candidate -> curated -> stable -> stale -> refreshed
```

### candidate
- research 근거는 있으나 latest-doc cross-check가 아직 완전하지 않다
- source는 모였지만 아직 canonical로 부르지 않는다

### curated
- official docs, approved GitHub refs, practical source가 맞물린다
- required fields complete
- testing baseline / anti-pattern / reject rule이 정리된다

### stable
- 최소 1회 내부 PoC 또는 실제 consumer usage 검토를 통과했다
- 반복 적용 가능한 품질이 입증됐다

### stale
- chub/latest-doc mismatch
- anti-pattern drift
- 반복 reject
- critical trigger 발생

### refreshed
- refresh workflow 수행
- freshness check 통과
- reject simulation 통과
- 필요한 경우 stable 재판정

---
## 7. Canonical Acceptance Gates
어떤 template도 아래 gate를 통과하기 전에는 canonical이 될 수 없다.

### Gate 1. Official Truth Gate
- official_doc_checked = true
- official docs와 latest-doc mismatch 없음
- source precedence는 항상 latest official docs 우선

### Gate 2. Provenance Gate
- official_doc_refs 존재
- approved_github_refs 존재
- practical reference 또는 internal reference 존재
- last_reviewed_at 존재

### Gate 3. Boring Default Gate
- experimental / alpha / beta / RC 의존 금지
- framework built-in으로 해결 가능한 걸 과하게 custom 구현하지 않음
- enterprise 운영자가 3년 유지 가능한 boring 선택 우선

예:
- Spring Boot는 patch maturity와 LTS alignment 확인
- React/Vercel은 experimental flag 없는 stable path 우선

### Gate 4. Testing & Quality Gate
- testing baseline 존재
- quality gates 존재
- verification checkpoints 존재
- zero-warning strictness를 목표로 한다

### Gate 5. Reject & Anti-Pattern Gate
- `must_not` 존재
- `reject_if` 존재
- anti-patterns 정의됨
- consumer contract 상 즉시 reject가 가능한 상태

### Gate 6. Asset Shape Gate
template는 최소 아래를 모두 가져야 한다.
- pack spec
- manifest
- shared fragments
- config/template assets

### Gate 7. Real Use Gate
- 최소 1회 internal PoC 또는 실제 usage 검토
- stable은 문서 검토만으로 부여하지 않는다

---
## 8. Stable Enough vs Latest Enough
### latest enough
- 최신 공식 문서와 충돌하지 않는다
- deprecated path를 강제하지 않는다
- current stable ecosystem과 맞는다

### stable enough
- 실제 PoC/consumer usage 검토를 통과했다
- 테스트 baseline과 verify가 존재한다
- 운영/유지보수 관점에서 boring하다

핵심:
> latest라고 stable이 아니다.  
> stable이라고 forever fixed도 아니다.

따라서 `ax-template`는 항상 **latest-enough + stable-enough**를 동시에 요구한다.

---
## 9. Repo Update Tracking Policy
외부 reference repo는 canonical이 아니지만, 계속 추적해야 한다.
그렇지 않으면 stale drift를 놓친다.

### 추적 대상
- 공식 문서 release notes
- approved reference repo release / semantic tags / changelog
- maintainer activity와 meaningful commit cadence
- security advisories / CVEs
- issue spike와 release 직후 문제 증가
- dependency churn
- breaking change signals
- 구조적 변경이 일어나는 파일 (`Dockerfile`, `package.json`, `.github/workflows/`, `tsconfig`, build config 등)

### watch rules
1. **Release Watch**
   - semantic release tag 기준으로 major / minor / patch를 구분해 본다
   - pre-release, alpha, beta는 채택 후보에서 제외한다
2. **Structural Change Watch**
   - 구조적 파일 변경만 추적한다
   - 단순 docs/example 변경은 canonical 판단 근거로 과대평가하지 않는다
3. **Activity Watch**
   - 최근 meaningful commit 부재, maintainer inactivity, issue backlog 급증 확인
4. **Security Watch**
   - CVE, advisory, deprecation notice 확인
5. **Drift Watch**
   - 우리 canonical pack과 공식 docs 사이 mismatch 확인
   - upstream architecture 변화가 내부 boring default를 흔드는지 확인

### review cadence
- **Patch / Security**: 주간 검토 또는 즉시 검토
- **Minor**: 월간 또는 정기 cadence 검토
- **Structural / Architectural**: 분기별 sync window에서만 검토
- **Major release**: 최소 30일 baking period 후 검토

즉 dependency freshness와 architecture sync를 같은 속도로 가져가지 않는다.

### stale triggers
아래 중 하나면 stale 후보로 본다.
- chub/latest-doc mismatch
- upstream maintenance 중단 징후
- critical CVE 미해결
- anti-pattern drift 발견
- repeated reject 증가
- 의미 있는 upstream commit 부재 6개월 이상
- release 이후 issue spike가 구조 안정성 문제를 시사함

### action policy
- stale 후보가 되면 자동 승격/동기화를 중단한다
- 필요 시 상태를 `MAINTAINED_INTERNALLY`로 전환한다
- upstream 변경을 reject한 경우, 이유를 `SYNC_DECISIONS.md` 또는 equivalent log에 남긴다
- bleeding-edge 기술이 필요하면 stable canonical을 흔들지 않고 별도 experimental template로 분리한다

---
## 10. Update / Defer / Deprecate Policy
### Adopt
- security patch는 빠르게 반영 검토
- minor update는 baseline tests + verify 통과 시 반영 가능
- major update는 cooling period 후 검토

### Defer
- ecosystem support가 아직 불안정함
- 실전에서 footgun이 많은 초기 버전
- enterprise/SI 운영 비용이 더 커지는 변화

### Deprecate
- upstream abandoned
- critical CVE 지속
- 공식 문서 기준에서 오래 벗어남
- internal canonical보다 더 이상 설명 가치가 없음

---
## 11. Template Families in ax-template
### scaffold
실제 시작 코드와 구조를 제공한다.
예: React + Spring Boot auth blueprint

### architecture_baseline
구조 규칙과 경계 기준을 제공한다.
예: auth boundary, state boundary, contract source-of-truth rules

### quality_companion
테스트/config/checklist/verify 기준을 제공한다.
예: verification checkpoints, CI quality gate, env validation

canonical template는 이 3층을 함께 가져야 한다.

---
## 12. Automation Rule
자동화는 외부 repo를 직접 읽고 따라가지 않는다.

자동화 기준은 항상 아래뿐이다.
1. curated or stable internal canonical template
2. canonical manifest
3. canonical reject / must_not / verify rule

즉,
> 외부 repo는 discovery input이다.  
> internal canonical template만 automation source다.

---
## 13. Immediate Action for ax-template
`ax-template`에서 다음 작업은 아래 순서로 간다.

1. 현재 V1 auth blueprint에 대해 pack family 분해
   - scaffold
   - architecture baseline
   - quality companion
2. auth blueprint용 candidate sources 목록 작성
3. reference implementation 선정표 작성
4. candidate manifest 초안 작성
5. chub freshness gate 연결
6. internal PoC 후 curated/stable 판정

---
## 14. Canonical Rule Summary
한 줄로 정리하면 이 문서의 핵심은 아래다.

> 우리는 star repo를 템플릿으로 쓰지 않는다.  
> 우리는 여러 후보를 모으고, 하나의 reference implementation을 고르고, 그것을 공식 문서 truth / reject rule / 테스트 / verify / 실제 적용 검토로 정규화한 뒤, stable internal canonical template로 승격해서 사용한다.
