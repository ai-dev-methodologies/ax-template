# GAP REPORT — ax-template Self-Validation

**Date**: 2026-04-04
**Validator**: ax-validation-test (dummy project)
**ax-template version**: commit 5185bfa
**Spec files used**: specs/auth-asvs-l1.yaml, contracts/auth-openapi.yaml, blueprints/auth-manifest.yaml

## Summary

| Metric | Value |
|--------|-------|
| Spec files copied | 3 |
| ASVS items tested | 6 / 23 |
| Endpoints implemented | 4 (signup, login, logout, /auth/me) |
| Feedback loop | **WORKS** — VIOLATION tests caught both V2.1.1 and V2.2.1 |
| Time to first GREEN | ~2 hours (estimated) |

## Portable ✅

Items that transferred cleanly from ax-template to new project:

- **ASVS spec YAML** (`specs/auth-asvs-l1.yaml`): All 6 tested items had clear requirement text. The `notes` field provided concrete test scenarios (e.g., "11-char password → 400").
- **OpenAPI contract** (`contracts/auth-openapi.yaml`): Endpoint paths, HTTP methods, request/response schemas were immediately usable. No ambiguity in the 4 implemented endpoints.
- **@Tag convention** (`@Tag("ASVS")` + `@Tag("ASVS-V2.1.1")`): Directly reusable. `testAsvs` Gradle task pattern worked identically.
- **Password validation rules** (V2.1.1, V2.1.2, V2.1.4, V2.1.9): Spec + contract together were sufficient to write tests without any external reference.

## Partially Portable ⚠️

Items that needed interpretation or additional context:

- **Rate limit values** (V2.2.1): ASVS spec says "rate limit" but not the exact threshold. Had to read `blueprints/auth-manifest.yaml` to find "5 per 15 min". **The spec alone is insufficient for this item.**
- **Error response schemas**: Contract defines some error shapes but not all. Had to infer 401 vs 429 status codes from context.
- **Signup response schema**: Contract shows `userId` + `message` but doesn't specify whether duplicate email should return 201 or 409. Chose 201 (enumeration prevention) based on ASVS principles, not spec.

## Not Portable ❌

Items that could NOT be used from the spec files:

- **JUnit test code**: ax-template's `SignupAsvs21Test.java` etc. are tightly coupled to `com.ax.template.authblueprint` package, MockMvc, and internal repository access. Cannot be reused.
- **JWT implementation details**: No spec/contract/manifest guidance on signing algorithm, key management, or token format. Had to choose HS256 independently.
- **Logout invalidation mechanism**: Spec says "logout invalidates session" (V3.3.1) but doesn't specify HOW. Had to invent in-memory token blocklist. A real project might use Redis, DB, or refresh token revocation.

## Missing from Spec ❓

Items needed during implementation that were absent from all 3 spec files:

1. **JWT signing algorithm and key management**: Not mentioned anywhere. Critical for security.
2. **Session invalidation mechanism for stateless JWT**: Spec says "invalidate" but JWT is stateless by design. No guidance on blocklist, refresh token revocation, or short-lived tokens.
3. **Email verification flow for tests**: Spec requires `emailVerified=true` for login, but no test-mode bypass or seeding mechanism is defined. Had to use direct DB seeding (`UserRepository.save()`).
4. **Token delivery for verification**: Spec mentions verification tokens but not how they reach the user (email? response body? console log?).
5. **CORS configuration**: Not in spec/contract/manifest. Required for any real frontend integration.
6. **Error response body schema**: Contract defines some but not all error responses. Inconsistent.

## Spec Interpretation Decisions

Every moment where the developer had to make a judgment call:

1. **V2.1.2**: "at least 64 chars permitted" — interpreted as "any password ≥ 64 chars is allowed" (not exactly 64). Spec notes confirm "64-char → 200".
2. **V2.2.1**: Rate limit threshold — required manifest. Spec text alone: "anti-automation controls are effective at mitigating credential stuffing" (no number). Spec notes say "5 failed per 15 min" matching manifest.
3. **Duplicate email signup**: Chose 201 (same response) for enumeration prevention. Spec doesn't specify.
4. **Logout HTTP method**: Contract says POST, not DELETE. Followed contract.
5. **JWT expiry**: Chose 1 hour. Not specified in spec/contract/manifest.
6. **Rate limit window reset**: Chose sliding window. Not specified.

## Moments Where ax-template Source Was Needed

Moments during implementation where the developer wanted to look at ax-template's Java code:

1. **JWT implementation**: "How did ax-template handle JWT signing?" → Resisted, chose HS256 independently.
2. **Logout invalidation**: "How did ax-template invalidate tokens?" → Resisted, invented blocklist.
3. **SecurityConfig setup**: "What does ax-template's SecurityConfig look like?" → Resisted, wrote from scratch. Hit Spring Security `/error` endpoint issue — had to add `.requestMatchers("/error").permitAll()`.
4. **Rate limiter implementation**: "How did ax-template implement the sliding window?" → Resisted, wrote from scratch.

**Result**: All 4 moments were resisted. The spec/contract/manifest was sufficient to implement WITHOUT looking at ax-template source. However, the implementations may differ significantly from ax-template's approach.

## Feedback Loop Verification

| Test | VIOLATION | Detection |
|------|-----------|-----------|
| V2.1.1 (password min 12) | Changed `@Size(min = 12)` → `@Size(min = 4)` | ✅ testAsvs FAILED — `asvs_V2_1_1_passwordMinLength12_rejectsShorter()` |
| V2.2.1 (rate limit 5/15min) | Changed `MAX_ATTEMPTS = 5` → `MAX_ATTEMPTS = 9999` | ✅ testAsvs FAILED — `asvs_V2_2_1_rateLimitAfter5FailedAttemptsIn15Min()` |

**Conclusion**: The feedback loop WORKS. When implementation violates the spec, `./gradlew testAsvs` catches it immediately.

## Recommendations for ax-template

1. **Add JWT guidance to manifest**: Specify signing algorithm, key rotation policy, token expiry.
2. **Define session invalidation strategy**: Specify whether to use blocklist, refresh token revocation, or short-lived access tokens.
3. **Add test-mode seeding mechanism**: Define how tests should create verified users without going through the full email flow.
4. **Standardize error response schema**: Add a common error schema to the OpenAPI contract.
5. **Add CORS policy to manifest**: Specify allowed origins, methods, headers.
6. **Make V2.2.1 spec self-sufficient**: Embed the rate limit values (5/15min) directly in the ASVS spec notes, not just the manifest.
7. **Consider portable test templates**: Provide RestAssured test templates (not MockMvc) that can be copied into any project.
