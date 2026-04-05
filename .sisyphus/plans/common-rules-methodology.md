# Common Rules Extraction + Methodology Documentation

## TL;DR

> auth 템플릿에서 검증된 프로세스를 도메인 무관한 방법론 플레이북으로 문서화한다.
> 빈 YAML 스키마 템플릿 + 5단계 프로세스 + auth를 worked example로 참조.
> `specs/common/` 같은 선제적 공통 규칙은 만들지 않는다 (두 번째 템플릿 전까지).
>
> **Estimated Effort**: Quick (2-3h)

---

## TODOs

- [ ] 1. METHODOLOGY.md (Steps 1-3): Spec Trio 정의 + 빈 YAML 스키마 템플릿 3종

  **What to do**:
  - 프로젝트 루트에 `METHODOLOGY.md` 생성
  - **Step 1: Compliance Spec 스키마 템플릿** — `specs/{domain}-{standard}.yaml` 빈 구조
    - fields: version, scope, stack, items[].{id, chapter, requirement, test_method, verification_type, applicable, notes, policy_ref}
    - auth 예시 경로만 참조 (`specs/auth-asvs-l1.yaml` 참고)
  - **Step 2: OpenAPI Contract 스키마 템플릿** — `contracts/{domain}-openapi.yaml` 빈 구조
    - fields: openapi, info, servers, paths, components.schemas (ErrorResponse/ValidationErrorResponse 포함)
  - **Step 3: Policy Manifest 스키마 템플릿** — `blueprints/{domain}-manifest.yaml` 빈 구조
    - structural fields: when_to_use, not_for, must_not, reject_if, testing_baseline, verification_checkpoints, source_precedence
  - **모든 auth 내용 제거** — 스키마 구조만, auth ID/정책값 없음
  **Must NOT do**: auth 내용(ASVS, JWT, OAuth) 본문에 포함 금지. specs/common/ 생성 금지.

  **Acceptance Criteria**:
  - [ ] METHODOLOGY.md에 3개 빈 YAML 스키마 존재
  - [ ] `grep -ciE "(asvs|owasp|jwt|oauth|password)" METHODOLOGY.md` → 0 (본문에 auth 내용 없음)

  **Commit**: `docs: METHODOLOGY.md Steps 1-3 — Spec Trio schema templates`

- [ ] 2. METHODOLOGY.md (Steps 4-5): 테스트 + 검증 + TDD 루프

  **What to do**:
  - **Step 4: Portable Test Template** — RestAssured 기반 검증 테스트 패턴
    - `@SpringBootTest(webEnvironment = RANDOM_PORT)` + RestAssured `given().when().then()`
    - `@Tag("{DOMAIN}")` + `@Tag("{DOMAIN}-{ITEM-ID}")` 컨벤션
    - 테스트 메서드 명명: `{domain}_{itemId}_{description}()`
  - **Step 5: Build Verification Gate**
    - Gradle task 템플릿: `tasks.register<Test>("test{Domain}") { useJUnitPlatform { includeTags("{DOMAIN}") } }`
    - 단일 명령 검증: `./gradlew test{Domain}`
  - **TDD 피드백 루프**: RED → GREEN → VIOLATION 사이클 설명
  **Must NOT do**: auth-specific 테스트 코드 본문에 포함 금지.

  **Acceptance Criteria**:
  - [ ] Gradle task 템플릿 존재: `grep 'tasks.register' METHODOLOGY.md`
  - [ ] @Tag 컨벤션 존재: `grep '@Tag("{' METHODOLOGY.md`
  - [ ] RED→GREEN→VIOLATION 설명 존재

  **Commit**: `docs: METHODOLOGY.md Steps 4-5 — test patterns and verification gate`

- [ ] 3. METHODOLOGY.md (Appendix): Auth worked example + dry-run checklist

  **What to do**:
  - **Appendix A: Auth Template Worked Example**
    - auth 파일을 경로로만 참조 (내용 복사 금지)
    - "Step 1에서 만든 Compliance Spec → auth에서는 `specs/auth-asvs-l1.yaml`" 형태
  - **Appendix B: Dry-Run Checklist**
    - "새 도메인에 이 방법론을 적용할 때" 체크리스트
    - [ ] Spec Trio 3종 생성했는가?
    - [ ] 각 compliance item에 @Tag 테스트가 1:1 매핑되는가?
    - [ ] `./gradlew test{Domain}` 한 줄로 전체 검증 가능한가?
    - [ ] VIOLATION 테스트로 피드백 루프를 증명했는가?
  - **Anti-patterns 섹션**: 거버넌스 루프 경고 (CLAUDE.md 참조)
  **Must NOT do**: 승격(promote), 게이트(gate), 승인(approval) 용어 사용 금지.

  **Acceptance Criteria**:
  - [ ] Auth 파일 참조가 경로로만 되어 있음 (내용 복사 아님)
  - [ ] `grep -ciE "(promote|approval|gate|curated|stable|evidence.bundle)" METHODOLOGY.md` → 0

  **Commit**: `docs: METHODOLOGY.md Appendix — auth worked example and dry-run checklist`

- [ ] 4. CLAUDE.md 업데이트

  **What to do**:
  - CLAUDE.md에 `## Methodology` 섹션 추가
  - `METHODOLOGY.md` 참조 링크
  - Spec Trio 개념 한 줄 요약
  **Must NOT do**: METHODOLOGY.md 내용 복사 금지. 참조만.

  **Acceptance Criteria**:
  - [ ] `grep "METHODOLOGY.md" CLAUDE.md` → match

  **Commit**: `docs: CLAUDE.md — add Methodology reference`

---

## Success Criteria
- [ ] METHODOLOGY.md가 프로젝트 루트에 존재
- [ ] auth 내용이 본문에 없음 (appendix에서 경로 참조만)
- [ ] 거버넌스 용어 없음
- [ ] 3개 빈 YAML 스키마 + Gradle task + @Tag 패턴 포함
- [ ] CLAUDE.md가 METHODOLOGY.md 참조
