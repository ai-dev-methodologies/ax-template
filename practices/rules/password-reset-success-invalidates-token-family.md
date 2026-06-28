---
title: A successful password reset MUST invalidate the user's ENTIRE family of outstanding unused reset tokens — not just the consumed one
impact: HIGH
impactDescription: "If a reset only marks the single consumed token used, every other reset token already issued to that account (a second 'forgot password' click, a token phished or shoulder-surfed earlier, a stale link in an old email) stays live after the password has changed — a CWE-640 weak-recovery replay window where an attacker holding any earlier token can reset the password again and hijack the account"
tags:
  - authn
  - credential-recovery
  - password-reset
  - token-invalidation
  - replay-resistance
  - cwe-640
spec_ref: "specs/auth-asvs-l1.yaml#AUTH-RESET-FAMILY-001"
verification:
  type: review
  source: "specs/auth-asvs-l1.yaml"
  pattern: "on a successful reset using ANY valid reset token, the reset transaction atomically marks ALL of that user's outstanding unused reset tokens used (a single bulk UPDATE/DELETE keyed on user_id + token_type), so a previously issued, never-consumed reset token is rejected after the reset; tokens are referenced by userId (DDD reference-by-id), and the invalidation runs in the SAME @Transactional path as the password write"
upstream:
  - "https://cwe.mitre.org/data/definitions/640.html"
  - "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x11-V2-Authentication.md"
evidence:
  - source_type: external
    citation: "CWE-640: Weak Password Recovery Mechanism for Forgotten Password — Description"
    url: "https://cwe.mitre.org/data/definitions/640.html"
    quote: "The product contains a mechanism for users to recover or change their passwords without knowing the original password, but the mechanism is weak."
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "OWASP ASVS v4.0.3 — V2.7.3 Out of Band Verifier (single-use recovery tokens)"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x11-V2-Authentication.md"
    quote: "Verify that the out of band verifier authentication requests, codes, or tokens are only usable once, and only for the original authentication request."
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "OWASP ASVS v4.0.3 — V2.7.2 Out of Band Verifier (recovery token expiry)"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x11-V2-Authentication.md"
    quote: "Verify that the out of band verifier expires out of band authentication requests, codes, or tokens after 10 minutes."
    quoted_at: "2026-06-28"
---

## A successful password reset MUST invalidate the user's ENTIRE family of outstanding unused reset tokens — not just the consumed one

**Impact: HIGH — a reset that only burns the token it consumed leaves every other live reset token for the account valid, which is exactly the CWE-640 weak-recovery replay window**

A password-reset flow issues a short-lived bearer token, mails it, and lets the holder set a new password. The obvious-but-wrong invariant is "the token I just used is single-use" — so the handler marks that one row `used = true` and stops. The real invariant is stronger: **the act of completing a recovery retires the whole recovery attempt, which means every token in that user's outstanding-unused reset family must die at once.**

Why the single-token view is a hole. Reset tokens accumulate per account in normal use: a user clicks "forgot password" twice (two valid tokens), a token leaks from an old email or an over-the-shoulder glance, a help-desk re-issues one. ASVS V2.7.3 says recovery tokens are "only usable once, and only for the original authentication request" — but the *original authentication request* is the recovery, not the individual token. If consuming token A resets the password but leaves token B live, an attacker who captured B earlier can walk in afterward and reset the password again, locking out the legitimate owner. The original password change did nothing to close the door B opens. CWE-640 names this class precisely: a recovery mechanism that is "weak" because it does not fully retire the recovery surface.

The fix is one atomic family-invalidation in the same transaction as the password write:

1. **Validate the presented token** (exists, unused, unexpired, correct type) — unchanged.
2. **Bulk-invalidate the whole family** — a single `UPDATE ... SET used = true WHERE user_id = ? AND token_type = 'RESET' AND used = false`. This marks the consumed token AND every sibling unused reset token in one statement, keyed on `user_id` (reference-by-id — never hold a Java pointer to the User aggregate).
3. **Write the new password** in the same `@Transactional` method, so the password change and the family-invalidation commit or roll back together.

After the fix the behavioral proof is: issue two reset tokens for one user → reset with token1 → token2 is rejected as already-used/invalid (HTTP 400), even though token2 was never consumed.

**Incorrect — only the consumed token is retired; the sibling token survives the reset and can replay it:**

```java
@Transactional
public PasswordResetResponse resetPassword(String token, String newPassword) {
    VerificationToken vt = repo.findByTokenAndUsedFalse(token)
        .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));
    if (vt.getExpiresAt().isBefore(Instant.now())) throw new InvalidTokenException("Token expired");
    vt.setUsed(true);            // ❌ burns ONLY this row
    repo.save(vt);               // ❌ a second reset token issued earlier is still used=false → live
    accounts.resetPassword(vt.getUserId(), newPassword);
    return new PasswordResetResponse("Password reset successful.");
}
```

**Correct — a successful reset atomically retires the user's entire unused reset-token family:**

```java
// repository — one indexed bulk update, keyed on the user id (reference-by-id)
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update VerificationToken v set v.used = true "
    + "where v.userId = :userId and v.tokenType = :tokenType and v.used = false")
int markAllUnusedAsUsed(@Param("userId") UUID userId, @Param("tokenType") String tokenType);

@Transactional
public PasswordResetResponse resetPassword(String token, String newPassword) {
    VerificationToken vt = repo.findByTokenAndUsedFalse(token)
        .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));
    if (vt.getExpiresAt().isBefore(Instant.now())) throw new InvalidTokenException("Token expired");
    UUID userId = vt.getUserId();
    repo.markAllUnusedAsUsed(userId, "RESET");   // ✅ consumed token AND every sibling die together
    accounts.resetPassword(userId, newPassword); // ✅ same transaction as the password write
    return new PasswordResetResponse("Password reset successful.");
}
```

The negative test that proves it: request a reset twice for the same account so two unused `RESET` tokens exist; `POST /password-reset` with token1 → **200**; `POST /password-reset` with token2 (never used) → **400** already-used/invalid. Both tokens are now `used = true`. Verification is review-tier here, backed by the auth domain's `@Tag("ASVS")` behavioral test (`passwordReset_successInvalidatesEntireTokenFamily`) which exercises exactly this two-token replay path under `./gradlew testAsvs`.

Reference: [CWE-640: Weak Password Recovery Mechanism for Forgotten Password](https://cwe.mitre.org/data/definitions/640.html)

Reference: [OWASP ASVS v4.0.3 — V2 Authentication (V2.7 Out of Band Verifier)](https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x11-V2-Authentication.md)
