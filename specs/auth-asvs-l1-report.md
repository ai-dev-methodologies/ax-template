# ASVS L1 Compliance Summary

Stack: Spring Boot + React | Scope: Email + OAuth Auth

| ASVS ID | Requirement | Test Method | Type | Status |
|---------|-------------|-------------|------|--------|
| ASVS-V2.1.1 | User set passwords are at least 12 characters in length. | asvs_V2_1_1_passwordMinLength12_rejectsShorter | api_test | ✅ COVERED |
| ASVS-V2.1.2 | Passwords of at least 64 chars are permitted; >128 chars are... | asvs_V2_1_2_password64CharsAllowed_129Rejected | api_test | ✅ COVERED |
| ASVS-V2.1.3 | Verify that password truncation is not performed. | asvs_V2_1_3_noPasswordTruncation | api_test | ✅ COVERED |
| ASVS-V2.1.4 | Verify that any printable Unicode character, including space... | asvs_V2_1_4_unicodeAndEmojiAllowed | api_test | ✅ COVERED |
| ASVS-V2.1.6 | Verify that password change functionality requires the user'... | asvs_V2_1_6_passwordChangeRequiresCurrent | api_test | ✅ COVERED |
| ASVS-V2.1.9 | Verify that there are no password composition rules. | asvs_V2_1_9_noCompositionRules | api_test | ✅ COVERED |
| ASVS-V2.2.1 | Verify that anti-automation controls are effective at mitiga... | asvs_V2_2_1_rateLimit_5FailedPer15Min | api_test | ✅ COVERED |
| ASVS-V2.5.2 | Verify that security questions or 'knowledge-based authentic... | asvs_V2_5_2_noSecurityQuestions | code_review | ✅ COVERED |
| ASVS-V2.5.3 | Verify that password reset mechanisms do not reveal the curr... | asvs_V2_5_3_resetDoesntRevealPassword | api_test | ✅ COVERED |
| ASVS-V2.5.4 | Verify that there are no default passwords. | asvs_V2_5_4_noDefaultAccounts | code_review | ✅ COVERED |
| ASVS-V2.7.2 | Verify that verification tokens have a short validity period... | asvs_V2_7_2_verificationTokenExpiry24h | api_test | ✅ COVERED |
| ASVS-V2.7.3 | Verify that verification tokens are single-use. | asvs_V2_7_3_verificationTokenSingleUse | api_test | ✅ COVERED |
| ASVS-V3.1.1 | Verify that the application never reveals session tokens in ... | asvs_V3_1_1_noSessionTokenInUrl | api_test | ✅ COVERED |
| ASVS-V3.2.1 | Verify that the application generates a new session token on... | asvs_V3_2_1_newSessionTokenOnAuth | api_test | ✅ COVERED |
| ASVS-V3.3.1 | Verify that logout invalidates the session token. | asvs_V3_3_1_logoutInvalidatesSession | api_test | ✅ COVERED |
| ASVS-V3.4.1 | Verify that cookie-based session tokens have the 'Secure' at... | asvs_V3_4_1_cookieSecureFlag | api_test | ✅ COVERED |
| ASVS-V3.4.2 | Verify that cookie-based session tokens have the 'HttpOnly' ... | asvs_V3_4_2_cookieHttpOnlyFlag | api_test | ✅ COVERED |
| ASVS-V3.4.3 | Verify that cookie-based session tokens have the 'SameSite' ... | asvs_V3_4_3_cookieSameSiteAttribute | api_test | ✅ COVERED |
| ASVS-V3.7.1 | Verify that the application requires a full session for high... | asvs_V3_7_1_fullSessionRequired | api_test | ✅ COVERED |
| ASVS-V4.1.1 | Verify that the application enforces access control rules on... | asvs_V4_1_1_accessControlOnTrustedLayer | code_review | ✅ COVERED |
| ASVS-V4.1.5 | Verify that access controls fail securely. | asvs_V4_1_5_accessControlFailsSecurely | api_test | ✅ COVERED |
| ASVS-V4.2.1 | Verify that the application protects against IDOR vulnerabil... | asvs_V4_2_1_noIDOR | api_test | ✅ COVERED |
| ASVS-V4.2.2 | Verify that the application protects against CSRF vulnerabil... | asvs_V4_2_2_antiCSRF | api_test | ✅ COVERED |
| ASVS-V2.8.1 | Verify that OAuth state parameter is used to prevent CSRF at... | asvs_V2_8_1_oauthStateParameterPreventsCsrf | api_test | ✅ COVERED |
| ASVS-V2.8.2 | Verify that OAuth redirect URI is strictly validated against... | asvs_V2_8_2_oauthRedirectUriValidation | api_test | ✅ COVERED |
| ASVS-V2.8.3 | Verify that OAuth client secrets and tokens are not exposed ... | asvs_V2_8_3_oauthSecretsNotExposed | api_test | ✅ COVERED |

**Total**: 26 applicable items | **Covered**: 26 | **Missing**: 0
