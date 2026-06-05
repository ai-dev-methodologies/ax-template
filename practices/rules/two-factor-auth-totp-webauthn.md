---
title: Second-factor auth MUST use a real distinct factor (TOTP/WebAuthn) with secure enrollment, per-session verification, recovery codes, attempt limits, and session binding
impact: HIGH
impactDescription: "A 2FA implementation that does not bind the verified factor to the session lets an attacker who passed step one skip step two; one with no attempt limit lets a 6-digit TOTP be brute-forced; one with no recovery codes locks users out permanently when they lose the device; one that re-uses a single OTP is replayable. MFA only raises assurance when each piece is correct."
tags:
  - authentication
  - mfa
  - 2fa
  - totp
  - webauthn
  - security
spec_ref: "specs/two-factor-auth-l0.yaml#TFA-FACTOR-001"
verification:
  type: review
  source: "specs/two-factor-auth-l0.yaml#TFA-FACTOR-001"
  pattern: "Second-factor authentication MUST use a genuinely DISTINCT factor category — a TOTP authenticator (RFC 6238) or a WebAuthn/FIDO2 credential — not a second password or a knowledge question (TFA-FACTOR-001). Enrollment is a deliberate ceremony that provisions and confirms the factor (TOTP secret shown once + a confirming code; WebAuthn registration ceremony) before it is active (TFA-ENROLL-001). Verification is per-session: the factor is checked at login (or step-up) and a TOTP code is single-use within its time step — never replayable (TFA-VERIFY-001). The user is issued single-use recovery codes at setup for device loss (TFA-RECOVERY-001). Verification attempts are rate-limited / strictly attempt-limited so a 6-digit code cannot be brute-forced (TFA-RATE-LIMIT-001, composes ratelimit). The verified factor MUST be bound to the authenticated session so a caller who passed factor one cannot reach protected resources without factor two (TFA-SESSION-BIND-001). Reject a 'second password' as a factor, an unbounded verification attempt count, a reusable OTP, and a session marked authenticated before the second factor is verified."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc6238"
  - "https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html"
evidence:
  - source_type: external
    citation: "OWASP Multifactor Authentication Cheat Sheet — MFA definition"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html"
    quote: "Multifactor Authentication (MFA) or Two-Factor Authentication (2FA) is when a user is required to present more than one type of evidence in order to authenticate on a system."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "OWASP Multifactor Authentication Cheat Sheet — recovery codes"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html"
    quote: "Providing the user with a number of single-use recovery codes when they first setup MFA."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "RFC 6238 — TOTP: Time-Based One-Time Password Algorithm (Abstract)"
    url: "https://www.rfc-editor.org/rfc/rfc6238"
    quote: "This document describes an extension of the One-Time Password (OTP) algorithm, namely the HMAC-based One-Time Password (HOTP) algorithm, as defined in [RFC 4226], to support the time-based moving factor."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Second-factor auth MUST use a real distinct factor with secure enrollment, per-session verification, recovery, attempt limits, and session binding

**Impact: HIGH — Per OWASP, *Multifactor Authentication (MFA) or Two-Factor Authentication (2FA) is when a user is required to present more than one type of evidence in order to authenticate on a system* — the key word is *type*: a second password is not a second factor. The assurance MFA adds evaporates if any piece is wrong: if the verified factor is not bound to the session, an attacker who phished the password skips factor two; if verification has no attempt limit, a 6-digit TOTP is brute-forced in minutes; if there are no recovery codes, a lost phone is a permanent lockout; if an OTP is accepted twice, it is replayable. This rule governs the whole ceremony.**

There are six load-bearing requirements — the items of `specs/two-factor-auth-l0.yaml`, all governed by this rule.

**1. A real distinct factor (TFA-FACTOR-001).** The second factor is a genuinely different category — a TOTP authenticator app (RFC 6238: *an extension of the ... HMAC-based One-Time Password (HOTP) algorithm ... to support the time-based moving factor*) or a WebAuthn/FIDO2 credential (a possession/biometric factor). A second password or a knowledge-based question is NOT a second factor.

**2. Enrollment ceremony (TFA-ENROLL-001).** Activating a factor is a deliberate ceremony: for TOTP, the secret is shown once (QR/secret) and the user confirms with a generated code before it goes live; for WebAuthn, the registration ceremony binds a credential to the account. A factor is not active until enrollment is confirmed.

**3. Per-session verification, single-use code (TFA-VERIFY-001).** The factor is verified at login (and on step-up for sensitive actions). A TOTP code is valid only within its time step and is single-use — once accepted (or its step elapses) it cannot be replayed.

**4. Recovery codes (TFA-RECOVERY-001).** Per OWASP, *providing the user with a number of single-use recovery codes when they first setup MFA* — issued at enrollment, each usable once, for device-loss recovery, stored hashed like passwords.

**5. Attempt limits / rate limit (TFA-RATE-LIMIT-001).** Verification is strictly attempt-limited and rate-limited so a 6-digit code's 10^6 space cannot be brute-forced; repeated failures lock or throttle. Composes the ratelimit catalog.

**6. Session ↔ factor binding (TFA-SESSION-BIND-001).** The session is NOT marked fully authenticated until the second factor is verified; the verified factor is bound to that session. A caller who passed only factor one cannot reach factor-two-protected resources — the authorization check distinguishes "password-authenticated" from "MFA-authenticated".

**Incorrect — a "second password" as the factor; session authenticated before factor two; unbounded attempts:**

```java
public Session login(String user, String pw, String secondPw) {
    if (!passwords.verify(user, pw)) throw new AuthException();
    Session s = sessions.create(user);          // VIOLATION: session authenticated BEFORE second factor (TFA-SESSION-BIND-001)
    if (!passwords.verify(user + ":2", secondPw)) // VIOLATION: a second password is not a distinct factor (TFA-FACTOR-001)
        throw new AuthException();               // VIOLATION: no attempt limit → brute-forceable (TFA-RATE-LIMIT-001)
    return s;
}
```

**Correct — TOTP factor; session pending until verified + single-use code + attempt limit + bound:**

```java
public LoginResult login(String user, String pw) {
    if (!passwords.verify(user, pw)) throw new AuthException();
    return LoginResult.mfaRequired(mfaChallenge.start(user)); // session NOT yet authenticated (TFA-SESSION-BIND-001)
}
public Session verifyTotp(String challengeId, String code) {
    rateLimiter.check(challengeId);                           // strict attempt limit (TFA-RATE-LIMIT-001)
    Challenge c = mfaChallenge.load(challengeId);
    if (!totp.verifySingleUse(c.userSecret(), code))          // RFC 6238, single-use within time step (TFA-VERIFY-001)
        throw new AuthException();
    return sessions.createMfaAuthenticated(c.user());         // factor bound to session (TFA-SESSION-BIND-001)
}
// Enrollment shows the secret once + confirming code (TFA-ENROLL-001);
// setup issues hashed single-use recovery codes (TFA-RECOVERY-001).
```

Verification: review-tier. MFA correctness is a security property with no compile-time signal — a weak 2FA flow compiles and lets legitimate users in while leaving the bypass open. Verify by review against `specs/two-factor-auth-l0.yaml`: the second factor is TOTP/WebAuthn (not a password/question); enrollment confirms before activation; codes are single-use and verified per session; recovery codes are issued and single-use; verification is attempt-limited; the session is not authenticated until the factor is verified and the factor is bound to it. When a fork-receiver wires real ITs (replayed TOTP rejected; N+1 attempts locked; pre-MFA session denied protected resources), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [RFC 6238 — TOTP: Time-Based One-Time Password Algorithm](https://www.rfc-editor.org/rfc/rfc6238)

Reference: [OWASP — Multifactor Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html)
