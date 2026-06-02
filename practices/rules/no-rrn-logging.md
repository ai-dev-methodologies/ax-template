---
title: "RRN (주민등록번호) must never appear in any log statement at any level"
rule_id: no-rrn-logging
impact: CRITICAL
impactDescription: "RRN is Sensitive Personal Information under 개인정보보호법 §24; its appearance in application logs constitutes an unauthorized disclosure breach"
tags:
  - privacy
  - pii
  - rrn
  - observability
  - locked_constraint
provenance_class: locked_constraint
protects_template_id: templates/backend/error/GlobalExceptionHandler.java
failing_fixture_path: practices/evals/fixtures/no-rrn-logging/fail_rrn_in_log/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-OBS-003"
verification:
  guard: no_rrn_in_log_guard.sh
  source: "practices/evals/no_rrn_in_log_guard.sh (2026-06-01 audit — mechanizes this CRITICAL rule)"
  pattern: "no log.<level>(...) statement references a raw RRN — bare token rrn (word-bounded, so rrnHash/rrnMasked/rrnToken are allowed) or 주민. Self-tested against practices/evals/fixtures/no-rrn-logging/{pass,fail_rrn_in_log}."
evidence:
  - source_type: external
    citation: "개인정보보호법 제24조 — 고유식별정보의 처리 제한 (Korean Personal Information Protection Act §24 — Restrictions on Processing Unique Identification Information)"
    url: "https://www.law.go.kr/법령/개인정보보호법"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "KISA 개인정보 기술적·관리적 보호조치 기준 — 접속기록의 위변조방지 및 RRN 처리"
    url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "OWASP Logging Cheat Sheet — Data to exclude: sensitive personal identifiers must never be written to log files"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## RRN (주민등록번호) must never appear in any log statement at any level

**Impact: CRITICAL — 개인정보보호법 §24 classifies the Resident Registration Number as a unique identification information (고유식별정보); its unauthorized disclosure triggers mandatory breach notification and administrative penalties.**

Application logs are retained by aggregators, SIEMs, object-storage buckets, and developer workstations. Any `log.info(...)`, `log.debug(...)`, `log.warn(...)`, or `log.error(...)` statement that includes an RRN constitutes an unauthorized disclosure if any of those sinks are accessed by personnel without proper clearance.

This rule is a **locked constraint**: it derives from statute (개인정보보호법 §24) rather than engineering preference. It cannot be relaxed by project-level override.

**Incorrect — RRN written to INFO and DEBUG log levels:**

```java
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void registerUser(String name, String rrn) {
        // VIOLATION: RRN in log — 개인정보보호법 §24 breach
        log.info("registering user {} with RRN: {}", name, rrn);
        log.debug("verifying identity for rrn={}", rrn);
    }
}
```

**Correct — log a non-sensitive identifier only:**

```java
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void registerUser(String name, String rrn) {
        // CORRECT: only the name (non-sensitive) is logged
        log.info("registering user name={}", name);
        // RRN processed in-memory and never emitted to any log sink
    }

    public void verifyIdentity(String rrn) {
        // CORRECT: log the outcome, not the RRN
        log.debug("identity verification attempted");
        boolean result = doVerify(rrn);
        log.info("identity verification result={}", result);
    }
}
```

## Why this matters

개인정보보호법 §24 imposes:
- Mandatory consent before collecting unique identification information
- Processing restrictions: only the minimum necessary for the stated purpose
- **Disclosure prohibition**: unauthorized disclosure (including to a log aggregator) triggers notification duties and fines up to ₩30M per violation

Application logs flow to: log aggregators (ELK/OpenSearch), S3 retention, developer terminals, CI artifact stores. None of these are controlled personal-information processing systems under §24.

The safe default is to **never log the RRN**, not to try to redact it downstream. Log scrubbers are best-effort and routinely bypass new fields.

## Failing fixture

See: `practices/evals/fixtures/no-rrn-logging/fail_rrn_in_log/UserService.java` — `log.info` and `log.debug` statements containing the `rrn` variable. A static analysis guard scanning for `log\.\(info\|debug\|warn\|error\).*\brrn\b` (word-bounded, so the allowed `rrnHash`/`rrnMasked`/`rrnToken` forms are NOT flagged) catches both.

Reference: [개인정보보호법 제24조 — 고유식별정보의 처리 제한 (Korean Personal Information Protection Act §24)](https://www.law.go.kr/법령/개인정보보호법)

Reference: [OWASP Logging Cheat Sheet — Data to exclude](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude)
