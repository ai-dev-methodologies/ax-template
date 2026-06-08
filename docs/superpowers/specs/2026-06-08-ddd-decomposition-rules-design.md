# ax-template — DDD 분해 · 패키지 구조 강제 규칙 (설계 스펙)

- 날짜: 2026-06-08
- 상태: 승인됨 (브레인스토밍 → 설계). 다음 단계 = 구현 플랜(writing-plans).
- 범위: 백엔드 Java/Spring. (프론트엔드 분해는 별도 스펙으로 분리.)
- 검증 과정: Exa 리서치(5주제) → 기존 52 기능 패키지 census → 룰셋 합성 → 적대적 검토(33 real findings) → codex 리뷰(CONDITIONAL-GO). 본 문서는 셋을 모두 반영한 최종본.

---

## 1. 문제 (갭)

ax-template는 **런타임 불변식**(보존·게이트·동시성 ~187 룰)은 기계적으로 강제하지만, **구조적 분해**는 강제하지 않는다. 현재 강제되는 구조 규칙은 **레이어 방향**(controller→service→repository, 이름 접미사 기반)·**패키지 비순환**·**리포지토리 형태**·thin-controller 위생·billing↔payment 특정 import 금지 뿐이다.

다음은 **규칙으로 강제되지 않는다** (= AI/포크 수신자가 일관성 없이 짤 수 있는 갭):
1. **분해 기준** — 합칠지 나눌지. 엔드포인트별 컨트롤러, god-service, 쿼리별 repo를 막는 규칙이 없음. "엔드포인트가 아니라 애그리거트로 묶어라"가 어디에도 없음.
2. **패키지 구조 방법론** — package-by-feature가 관행일 뿐 규칙이 아님. by-layer로 짜도 모든 게이트 통과.
3. **애그리거트/바운디드 컨텍스트 경계** — 묶음의 단위가 정의돼 있지 않음.
4. **일반 cross-feature 결합** — billing↔payment 특정 1건만 있고 일반 규칙 없음.

## 2. 방법론 (확정)

**DDD-lite.**
- **애그리거트** = 트랜잭션/일관성 경계 (Vernon: "an aggregate is synonymous with a transactional consistency boundary"). 묶는 근거는 "관련 있어서"가 아니라 "한 원자 트랜잭션에서 같이 일관성을 지켜야 해서".
- **바운디드 컨텍스트** = 기능 패키지 (`com.ax.template.authblueprint.<feature>`). **한 컨텍스트가 여러 애그리거트 루트를 가질 수 있음** (codex: dispatch = Provider/Offer/ServiceRequest 3 roots가 한 컨텍스트 — one-root-per-package를 인코딩하지 말 것).
- 애그리거트 간 참조는 **객체 포인터가 아니라 식별자(ID)** 로. tx당 애그리거트 1개 수정(나머지는 이벤트/eventual).
- **리포지토리는 애그리거트 루트당 1개** (멤버 엔티티엔 repo 없음 — 원칙).

**접근법: A(손수 ArchUnit 하드 가드) + 선택적 B(마커).** Spring Modulith 미도입 (카탈로그의 "프레임워크 강요 금지" 철학; 73개 기존 손수 가드와 일관). 기존 52 패키지를 깨지 않음 — 깨는 건 명시적 부채로 grandfather + 점진 마이그레이션.

## 3. 마커 (선택적 B) — 2개

순수 네이밍으로는 root vs member를 구분할 수 없다 (Order vs OrderItem 둘 다 PascalCase @Entity). codex 핵심 지적: `@AggregateRoot`만으로는 "멤버가 어느 루트 소속인지"도 인코딩 못 함 (ApprovalStep.request는 합법 child→root backref). 따라서 **마커 2개**:

- `common.AggregateRoot` — `@Target(TYPE) @Retention(RUNTIME)`, 멤버 없음. 각 애그리거트 **루트** 엔티티에만.
- `common.AggregateMember(Class<?> root)` — 멤버 엔티티에. `root`로 소속 루트를 명시 → 멤버→루트 map 완성.

ArchUnit이 바이트코드에서 읽음(의존성 없음). 이 두 마커는 **딱 세 가드**(HG-AGG-REPO / HG-AGG-REF / HG-AGG-MEMBER-ENCAP)를 위해서만 존재; 나머지는 위치+접미사로 강제(마커 불필요).

근거: Vernon(consistency boundary), Fowler("any references from outside the aggregate should only go to the aggregate root"), MS DDD guide(IAggregateRoot 마커가 repository 타입 제약).

## 4. back-tag wave (강제 prerequisite — IMW)

codex 확인: `AggregateRoot.java`·`AggregateMember.java`·`aggregate_boundary_allowlist.yaml` 모두 **현재 부재**. 마커 의존 가드는 root/member map 없이는 day-1에 false-positive/누락. 따라서:

- **1회 검토된 wave**: 52 기능 패키지의 **모든 @Entity**를 전수조사 → 각각 root/member 판정 → 마커 부착 + `aggregate_boundary_allowlist.yaml`에 근거 1줄 기록.
- census가 일부 오류가 있었으므로(아래 §8) **이 wave가 엔티티/루트/멤버 인벤토리의 단일 진실 소스**. (census의 "Phi/ConsentRecord 2 엔티티" 같은 주장은 wave에서 실측 검증.)
- **기능별 "tagged" 충족 전엔 마커 의존 가드(TIER-1)를 평가하지 않음** (부분 태깅 오탐 방지).
- **flip-forcing 가드**: 태깅 진행률을 세서 회귀하면 build fail → advisory가 영구화되지 않게.

## 5. allowlist 아티팩트 — `practices/evals/aggregate_boundary_allowlist.yaml` (schema 검증)

loophole 방지(codex): 각 예외는
```yaml
shared_kernel: [com.ax.template.authblueprint.common.**, ...]   # 안정적 kernel만 wildcard 허용
published_api:           # 기능별 published 표면 (@PublishedApi 마커 또는 명시)
  payment: [PaymentService, CreatePaymentRequest, PaymentProvider]
exceptions:              # grandfather / composition 등 — 정확한 클래스, wildcard 금지
  - from: com.ax.template.authblueprint.auth.RefreshToken
    to:   com.ax.template.authblueprint.user.UserEntity
    kind: cross-aggregate-fk
    owner: <name>
    rationale: "auth↔user 강결합 (deep)"
    expiry: 2026-09-30
    remediation_ticket: AX-DDD-AUTH-USER
```
CI 검증: (1) 모든 from/to가 **실재 클래스로 resolve** 되는지, (2) **만료(expiry 경과) 또는 미사용(더 이상 위반 없음) 항목을 fail** → 예외가 영구 escape hatch가 되지 않게.

## 6. 하드 가드 (TIER-0: 즉시 block / TIER-1: back-tag 후 advisory→block)

> codex 검증: HG-FEAT-NOCYCLE만 그대로 WORKS, 나머지는 아래 수정 후 작성. 각 가드는 §9 evidence에 근거.

### TIER-0 (비마커, 즉시 block — false-positive 수정 완료)
- **HG-FEAT-TOPLEVEL-TECH** — base package(동적 계산, 5th-segment 가정 금지) 직속 자식에 by-layer/by-endpoint 패키지명(`controllers/`,`services/`,`repositories/`,`routes/`,`models/`) **금지**. *"기능은 persistence(@Entity/@Repository) 필수" 조건은 두지 않음* — controller/service-only 기능 합법(apiversioning, caching, i18n). (kernel/cross-cutting 패키지는 allowlist.)
- **HG-FEAT-NOCYCLE** — 기능 슬라이스 간 비순환. 기존 `ArchitectureNoCyclicPackageTest`(올바른 slices().beFreeOfCycles() 형태) 유지 + stale 주석 정정.
- **HG-KERNEL-NO-FEATURE-DEP** — kernel(common, observability)은 leaf feature 의존 금지. security→feature carve는 **`jakarta.servlet.Filter` 할당 가능 타입 한정**(ApiKeyAuthenticationFilter는 OncePerRequestFilter) — 전체 feature carve 아님.
- **HG-FEAT-ISOLATION** — cross-feature 참조는 (i) 대상 기능 **published-API**(`@PublishedApi` 마커 또는 allowlist, **default-deny**; "ends in Service" 휴리스틱 폐기), (ii) shared kernel, (iii) allowlist된 composition/grandfather 만. 타 기능 @Entity/*Repository BLOCK. (prerequisite: `@PublishedApi` 마커 + YAML 작성.)
- **HG-ANTI-SPLIT-ENDPOINT (축소)** — codex: lexical도 path-shape도 false-positive/gameable(PaymentAdminController가 /payments+/reconciliation, DispatchController 광범위). → **TIER-0에는 느슨한 lexical 금지만**: 동사형 컨트롤러명(`Create*Controller`,`List*Controller`,`Get*Controller`) 금지. "리소스 응집/엔드포인트 분할" 적정성은 **TIER-2 review로 강등**(기계화 시 오탐·우회 둘 다 발생).

### back-tag wave 완료 후 → TIER-1 (마커 의존, advisory→block, forcing function)
- **HG-AGG-REPO** — `@AggregateRoot`당 repository 1개; 멤버 엔티티 repo 없음. **breaks_existing=TRUE 확정** (codex: ApprovalStep/CommentEdit/SubjectMember/NetPosition/GrossObligation **5개 member-repo 실재**). wave에서 각각 **root 승격** 또는 **allowlist 명시 예외**로 해소 후 flip.
- **HG-AGG-REF** — 필드 타입이 `@AggregateRoot`이고 **선언 엔티티 자신의 애그리거트(자기 root)와 다른 root**면 금지. child→own-root backref 허용(ApprovalStep.request 합법). @OneToMany/@ManyToMany **컬렉션 element 타입 해석**. grandfather(auth FK)는 allowlist.
- **HG-AGG-MEMBER-ENCAP** — `@AggregateMember` 엔티티는 소유 기능 밖에서 참조 금지(루트만 외부 노출).
- **HG-ANTI-GODSERVICE-TX** — codex 결정 = **transitive 호출그래프 분석 미채택**(Spring tx 프록시/propagation/event/dirty-checking 때문에 overblock 또는 우회). → **shallow 휴리스틱**: 한 `@Transactional` 메서드 본문이 직접 ≥2개 **distinct @AggregateRoot 타깃**(repo 수 아님)을 mutate(save/delete)하면 스멜 → governed allowlist(OrderService.checkout 기준 케이스: cart/product/payment/notification/order 조율). **정직한 한계 명시**: helper/service 위임을 통한 우회는 못 잡음 → 그건 TIER-2 review.
- **HG-STATE-SOLE-MUTATOR** — *StateMachine 보유 기능에서, 상태 필드(@Enumerated — `status` **또는 `state`** 둘 다; codex: Payment는 `state`)를 쓰는 mutator를 state machine 경유로 제한. **public setter 오버블록 회피**(Product.status public setter가 ecommerce에 존재).

## 7. TIER-2 (인간 판단) + 스캐폴드

**TIER-2 review-tier 룰(하드 가드 불가):** 애그리거트 경계 적정성(multi-root 기능이 정말 한 컨텍스트인가) · cross-feature 의존 merge vs split · TX 휴리스틱 너머 god-service · 엔드포인트 분할 응집성 · 각 grandfather remediation 여부.

**스캐폴드(docs/NEW-DOMAIN-CHECKLIST.md 확장):** §1.1 root @AggregateRoot + 멤버 @AggregateMember + cross-aggregate는 id로 / §1.2 repo는 @AggregateRoot에만 / §1.3 @Transactional 메서드 ≤1 애그리거트 mutate, 교차는 published service·event / §1.4 컨트롤러 root당 1 thin `/api/<resource>` / §1.9 cross-feature는 published API만 / §4 enforcement 테이블에 신규 가드.

## 8. 결정사항 & census 정정 (codex)

- **god-service**: shallow + governed allowlist (transitive 미채택).
- **auth→user**: **grandfather (확장 범위)** + 후속 IMW remediate. codex: 결합이 draft보다 깊음 — `UserEntity`, `UserRepository`, `UserRole`, `ProviderLinkRepository`, `OAuthProvider`, RefreshToken/VerificationToken의 @ManyToOne FK. allowlist 예외에 전부 명시 + expiry + ticket.
- **back-tag wave**: 강제 선행(마커·YAML 부재 확인).
- **multi-root per feature**: 허용 (one-root-per-package 인코딩 금지).
- **allowlist**: schema 검증(§5).
- **census 정정**: `Phi`는 @Entity가 아니라 `@Retention(SOURCE)` annotation. common의 실제 엔티티 집합은 back-tag wave가 실측 확정. `integration`은 composition 아님(self-contained webhook). 실제 composition = `ecommerce`(+`dsr`).

## 9. evidence (근거 — 전부 권위 출처, 룰별 anchor)

- Vernon, *Effective Aggregate Design* I–III (dddcommunity.org PDF): consistency boundary, small aggregates, reference-by-identity, modify-one-aggregate-per-transaction.
- Evans, *DDD*: repository per aggregate root; root만 외부 global access.
- Fowler, *DDD_Aggregate* (martinfowler.com): "references from outside the aggregate should only go to the aggregate root."
- Microsoft *.NET DDD guide*: IAggregateRoot 마커가 repository 타입을 제약.
- ArchUnit User Guide / Spring Modulith docs: slices·no-cross-internal·published-API-only·no-cycle (개념 차용, 구현은 손수 ArchUnit).

## 10. 롤아웃 (phase)

1. **Phase 0 (즉시):** 마커 2개(`@AggregateRoot`,`@AggregateMember`,`@PublishedApi`) 파일 + allowlist YAML 스키마 + CI 검증 가드 생성. TIER-0 가드 5개 작성·block(false-positive 수정 포함). 기존 코드 전 GREEN 확인.
2. **Phase 1 (back-tag wave / IMW):** 52 패키지 root/member 전수 태깅 + allowlist 기록. flip-forcing 진행률 가드.
3. **Phase 2 (flip):** wave 완료 기능부터 TIER-1 마커 의존 가드를 advisory→block. HG-AGG-REPO의 5개 member-repo는 root 승격/예외로 해소 후 flip.
4. **Phase 3 (지속):** TIER-2 review 룰 + 스캐폴드 + grandfather remediation IMW(auth→user 등).

## 11. 성공 기준

- TIER-0 5개 가드가 block 모드로 기존 52 패키지 전부 GREEN(false-positive 0).
- 마커 2종 + allowlist YAML + CI 검증 가드 존재, `verify-completion`/run-all-guards에 통합.
- back-tag wave 완료 후 TIER-1 4개 가드 block, 기존 GREEN(예외는 allowlist에 expiry 포함).
- NEW-DOMAIN-CHECKLIST + headline/AGENTS 갱신.
- "엔드포인트별 분할"·"god-service"·"by-layer 패키지"·"cross-aggregate 객체참조"가 신규 코드에서 기계적으로 차단(또는 TIER-2에서 포착)됨을 reference 위반 fixture로 증명.

## 12. 범위 밖 (out of scope)

- 프론트엔드(React) 분해/패키지 규칙 — 별도 스펙.
- Spring Modulith 도입 — 미래 옵션(repo에 fixture만).
- auth→user 실제 remediation 코드 — 후속 IMW (본 스펙은 grandfather + ticket까지).
- 애그리거트-size 예산 가드(heuristic) — 후속(연구엔 있으나 본 스펙 제외).
