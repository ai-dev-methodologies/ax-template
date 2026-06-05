---
title: Consent MUST be an explicit affirmative opt-in, withdrawable as easily as given, recorded with proof, and purpose-scoped
impact: HIGH
impactDescription: "Consent inferred from a pre-ticked box, silence, or a bundled blanket agreement is not valid consent under GDPR — processing on it is unlawful. Consent with no withdrawal path traps the user; consent with no recorded proof cannot be demonstrated to a regulator (the controller bears the burden); consent reused for a purpose it was not given for is a purpose-limitation breach. Each must be correct or the legal basis collapses."
tags:
  - consent
  - gdpr
  - privacy
  - opt-in
  - purpose-limitation
  - audit
spec_ref: "specs/consent-management-l0.yaml#CONSENT-CAPTURE-001"
verification:
  type: review
  source: "specs/consent-management-l0.yaml#CONSENT-CAPTURE-001"
  pattern: "Consent MUST be captured as an explicit affirmative opt-in — a clear affirmative act (an unchecked box the user ticks, an explicit accept), NEVER a pre-ticked box, silence, inactivity, or consent bundled into a blanket ToS acceptance (CONSENT-CAPTURE-001). The user MUST be able to withdraw consent at any time, and withdrawal MUST be as easy as giving it (CONSENT-WITHDRAW-001). Every grant and withdrawal MUST be recorded with proof — who, what purpose, when, the policy version, and the capture context — so the controller can demonstrate consent (CONSENT-RECORD-001). Consent is scoped to specific declared purposes; data MUST NOT be processed for a purpose the user did not consent to, and multiple purposes require separate consent (CONSENT-PURPOSE-001). Cookies/trackers are grouped into categories the user consents to independently, with non-essential defaulting OFF (CONSENT-COOKIE-001). A material policy/purpose version change requires re-consent; stale consent against an old version does not authorize new processing (CONSENT-VERSIONING-001). A minor below the capacity age requires guardian proxy consent (CONSENT-CAPACITY-001). Reject pre-ticked/implied consent, a withdraw flow harder than the grant flow, processing without a recorded consent row, and reuse of consent across unconsented purposes."
upstream:
  - "https://gdpr-info.eu/art-7-gdpr/"
  - "https://gdpr-info.eu/recitals/no-32/"
evidence:
  - source_type: external
    citation: "GDPR Article 7(1) — Conditions for consent (controller must demonstrate consent)"
    url: "https://gdpr-info.eu/art-7-gdpr/"
    quote: "Where processing is based on consent, the controller shall be able to demonstrate that the data subject has consented to processing of his or her personal data."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "GDPR Article 7(3) — Conditions for consent (withdrawal as easy as giving)"
    url: "https://gdpr-info.eu/art-7-gdpr/"
    quote: "It shall be as easy to withdraw as to give consent."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "GDPR Recital 32 — Conditions for consent (clear affirmative act; no pre-ticked boxes)"
    url: "https://gdpr-info.eu/recitals/no-32/"
    quote: "Silence, pre-ticked boxes or inactivity should not therefore constitute consent."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Consent MUST be an explicit affirmative opt-in, withdrawable as easily as given, recorded with proof, and purpose-scoped

**Impact: HIGH — When processing relies on consent as its legal basis, the consent must be real or the basis is void and the processing unlawful. GDPR Recital 32 is explicit that *silence, pre-ticked boxes or inactivity should not therefore constitute consent* — consent must be *a clear affirmative act*. Article 7 adds that the controller bears the burden of proof — *where processing is based on consent, the controller shall be able to demonstrate that the data subject has consented* — and that *it shall be as easy to withdraw as to give consent*. A consent flow that pre-ticks the box, hides the withdrawal, keeps no record, or reuses a grant for a purpose it was never given for fails one of these and collapses the legal basis.**

There are seven load-bearing requirements — the items of `specs/consent-management-l0.yaml`, all governed by this rule.

**1. Explicit affirmative opt-in (CONSENT-CAPTURE-001).** Consent is captured by a clear affirmative act — an unchecked box the user actively ticks, an explicit "I agree" — never a pre-ticked box, silence, inactivity, or consent buried inside a blanket ToS acceptance.

**2. Easy withdrawal (CONSENT-WITHDRAW-001).** The user can withdraw at any time, and withdrawal is *as easy as* giving — a one-click toggle in settings, not a support-ticket-and-wait. Withdrawal stops the consented processing going forward.

**3. Recorded proof (CONSENT-RECORD-001).** Every grant and withdrawal writes an immutable record — subject, purpose, timestamp, policy version, capture context (UI/IP/locale) — so the controller can *demonstrate* the consent on demand. No consent row ⇒ no demonstrable basis.

**4. Purpose limitation (CONSENT-PURPOSE-001).** Consent is scoped to specific declared purposes; processing for a purpose the user did not consent to is forbidden, and distinct purposes require separate consent (no single checkbox covering analytics + marketing + profiling).

**5. Cookie categories (CONSENT-COOKIE-001).** Cookies/trackers are grouped into categories (essential / functional / analytics / marketing); the user consents to each independently, and non-essential categories default OFF until affirmatively enabled.

**6. Re-consent on version change (CONSENT-VERSIONING-001).** A material change to the policy or purposes requires re-consent; a consent recorded against an old version does not authorize processing introduced by a new version.

**7. Capacity / guardian proxy (CONSENT-CAPACITY-001).** A minor below the applicable capacity age cannot self-consent; guardian (proxy) consent is required and recorded as such.

**Incorrect — pre-ticked, bundled, no record, no separate purposes:**

```html
<!-- VIOLATION: pre-checked (CONSENT-CAPTURE-001); one box bundling all purposes (CONSENT-PURPOSE-001) -->
<input type="checkbox" name="consent" checked>
I agree to the Terms, and to analytics, marketing, and profiling.
```
```java
// VIOLATION: processes on an implied flag, writes no consent record (CONSENT-RECORD-001)
if (user.tosAccepted) analytics.track(user);
```

**Correct — affirmative per-purpose opt-in, recorded with version, withdrawable, defaults off:**

```java
// Each purpose is a distinct, unchecked-by-default opt-in (CONSENT-CAPTURE-001 / CONSENT-PURPOSE-001 / CONSENT-COOKIE-001)
record ConsentGrant(String userId, Purpose purpose, boolean granted,
                    String policyVersion, Instant at, String context) {}

void capture(String userId, Map<Purpose,Boolean> choices, String policyVersion, String ctx) {
    choices.forEach((p, granted) ->
        consentLog.record(new ConsentGrant(userId, p, granted, policyVersion, clock.now(), ctx))); // proof (CONSENT-RECORD-001)
}
boolean mayProcess(String userId, Purpose p) {
    return consentLog.latest(userId, p)                       // purpose-scoped check (CONSENT-PURPOSE-001)
        .filter(g -> g.granted() && currentPolicy.equals(g.policyVersion())) // re-consent on version (CONSENT-VERSIONING-001)
        .isPresent();
}
// withdraw(userId, p) records a granted=false row via the SAME one-click surface (CONSENT-WITHDRAW-001).
```

Verification: review-tier. Consent validity is a legal/contract property with no compile-time signal — a pre-ticked flow compiles and collects "consent" that is legally void. Verify by review against `specs/consent-management-l0.yaml`: opt-in is an affirmative act (no pre-tick/bundling); withdrawal is as easy as the grant; every grant/withdrawal is recorded with purpose+version+timestamp; processing checks purpose-scoped consent; cookie categories default non-essential off; a version change forces re-consent; minors use guardian proxy. When a fork-receiver wires a real IT (process blocked without a consent row; withdrawal flips mayProcess to false), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [GDPR Article 7 — Conditions for consent](https://gdpr-info.eu/art-7-gdpr/)

Reference: [GDPR Recital 32 — Affirmative consent](https://gdpr-info.eu/recitals/no-32/)
