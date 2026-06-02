---
title: "Backend endpoints accepting a Korean Business Registration Number (사업자등록번호) must validate the input against the 10-digit NNN-NN-NNNNN format before persistence or logging"
rule_id: korean-brn-format
impact: HIGH
impactDescription: "Korean B2B integration endpoints (tax invoices, e-Tax, supplier onboarding, payment ledger) silently accept malformed BRN strings (truncated, free-form, including 주민등록번호 by mistake) when no format check runs at the controller boundary. The downstream effects — failed NTS reconciliation, mis-routed VAT, RRN leakage through a field reused as a BRN slot — surface only at audit time. A 10-digit NNN-NN-NNNNN regex enforced at the DTO layer rejects all four classes at the boundary."
tags:
  - validation
  - identity
  - brn
  - korean-compliance
  - locked_constraint
provenance_class: locked_constraint
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-001"
verification:
  type: review
  status: manual
  notes: "Static analysis: every backend DTO field semantically representing a 사업자등록번호 (commonly named brn, businessRegistrationNumber, businessNumber, 사업자등록번호, businessRegNo) must be wired to a Jakarta `ConstraintValidator` that applies the regex ^[0-9]{3}-[0-9]{2}-[0-9]{5}$ before any service-layer call. Inputs failing the regex must be rejected with HTTP 400 + RFC 7807 problem detail; never persisted in unvalidated form; never logged in raw form. The checksum algorithm (mod-10 weighted-sum) is intentionally OUT-OF-SCOPE for this rule (deferred R13+ as a separate rule contingent on an authoritative source landing) — see practices/DECISIONS.md TD-034 (korean-brn-checksum) deferral."
evidence:
  - source_type: external
    citation: "부가가치세법 (대한민국) — 사업자등록의 근거 법령. 사업자등록번호는 이 법에 따라 국세청(NTS)이 부여하는 사업자별 식별자이며, 사업자등록증에 10자리(3-2-5, XXX-XX-XXXXX) 형식으로 표기된다. (mod-10 가중합 체크섬은 본 룰 범위 밖 — korean-brn-checksum 룰로 분리 예정)"
    url: "https://www.law.go.kr/법령/부가가치세법"
    quoted_at: "2026-05-24"
decided_at: "2026-05-24"
---

## Backend endpoints accepting a 사업자등록번호 must validate the input against the 10-digit NNN-NN-NNNNN format

**Impact: HIGH — Korean B2B endpoints (세금계산서, e-Tax, supplier onboarding, payment ledger) silently accept malformed 사업자등록번호 input when no controller-boundary check runs. Downstream effects (failed NTS reconciliation, mis-routed VAT, accidental RRN leakage through a reused field) surface only at audit time.**

The 사업자등록번호 (Business Registration Number, BRN) is a 10-digit identifier issued by the 국세청 (National Tax Service) to every business entity registered in Korea. Its canonical display form is `NNN-NN-NNNNN` — 3-digit 세무서 code + 2-digit individual/corporate code + 5-digit sequence — and the same 10-digit shape is what `세금계산서 작성요령` requires on every issued tax invoice. The rule constrained here is **format-only**: any backend endpoint accepting a BRN field must run the regex `^[0-9]{3}-[0-9]{2}-[0-9]{5}$` (or the equivalent compact `[0-9]{10}` form normalised before validation) at the DTO layer before the service tier runs.

The **mod-10 weighted-sum checksum** that NTS publishes alongside the format is intentionally **out of scope** for this rule. R12 evidence collection on 2026-05-24 could not surface a verbatim Korean authoritative source for the checksum algorithm (위키백과 사업자등록번호 alt URL is 200 OK but its content does not cover the 10-digit format or the checksum; namu.wiki is bot-blocked; en.wikipedia "Business_registration_number" returns 404; law.go.kr / hometax.go.kr / NTS-7660 host-wide downgraded — see practices/upstream/r12-sp49-evidence-snapshot.md). A separate `korean-brn-checksum` rule is queued as `TD-034 korean-brn-checksum` in `practices/DECISIONS.md` and will ship once an authoritative source lands.

**Incorrect — DTO accepts arbitrary string in a BRN slot; service layer assumes well-formed input:**

```java
public record CreateSupplierRequest(
        @NotBlank String name,
        @NotBlank String brn,            // accepts "1234567890", "abc", a raw 13-digit RRN, "123-45-6789012", anything
        @NotBlank String contactEmail
) {}

@PostMapping("/api/suppliers")
public ResponseEntity<Void> create(@RequestBody @Valid CreateSupplierRequest req) {
    supplierService.register(req.name(), req.brn(), req.contactEmail());  // persisted unvalidated
    return ResponseEntity.created(URI.create("/api/suppliers/" + req.brn())).build();
}
```

**Correct — Jakarta ConstraintValidator runs at the DTO boundary; only the 10-digit NNN-NN-NNNNN shape proceeds to the service tier:**

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BusinessRegistrationNumberValidator.class)
public @interface BusinessRegistrationNumber {
    String message() default "BRN must match NNN-NN-NNNNN (3-2-5 digits)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public final class BusinessRegistrationNumberValidator
        implements ConstraintValidator<BusinessRegistrationNumber, String> {

    // Canonical Korean format: 3-digit 세무서 code · 2-digit individual/corporate code · 5-digit sequence.
    private static final java.util.regex.Pattern BRN =
            java.util.regex.Pattern.compile("^[0-9]{3}-[0-9]{2}-[0-9]{5}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) {
            return false;   // @NotNull is enforced separately; here, null = invalid BRN shape.
        }
        return BRN.matcher(value).matches();
    }
}

public record CreateSupplierRequest(
        @NotBlank String name,
        @NotBlank @BusinessRegistrationNumber String brn,
        @NotBlank @Email String contactEmail
) {}
```

The matching 400 response is shaped by the project's existing `GlobalExceptionHandler` (RFC 7807 ProblemDetail). No raw BRN appears in the error message — only a stable problem `type` URI (`urn:ax:supplier:invalid-brn`) and a sanitized property pointer, so the application logs do not leak the rejected value.

### Why "format-only" and not checksum

The mod-10 weighted-sum checksum NTS publishes is a stronger check (it rejects typos that pass the format gate), but R12 evidence collection on 2026-05-24 found no Korean authoritative source verbatim-reachable to anchor the algorithm. Shipping a checksum-coupled rule against vendor-blog reconstructions of the algorithm would fail the catalog's `evidence:` discipline (every normative claim must be sourced from a verbatim upstream — see `practices/AGENTS.md` evidence-anchored rule provenance contract).

R12 PRD §4.3 + practices/DECISIONS.md TD-034 explicitly defers `korean-brn-checksum` to a later cycle. The format-only rule still closes the four most common failure modes — truncated input, free-form text, an RRN pasted into a BRN slot, the wrong separator pattern.

### What this rule does NOT do

- It does not validate that the 3-digit prefix is a real 세무서 code (NTS publishes the list; the list rotates as offices reorganise — too volatile for a static rule).
- It does not validate the checksum (see above).
- It does not cover RRN (주민등록번호); that lives in `no-rrn-collection-without-legal-basis.md` and is a stricter legal-basis rule, not a format rule.
- It does not impose a storage encoding; teams choose between persisting the dashed form (`123-45-67890`) or the bare-digit form (`1234567890`) per their schema convention. Both shapes are valid as long as the controller-boundary regex accepts only the dashed canonical form on the wire.

Reference: https://www.law.go.kr/법령/부가가치세법
