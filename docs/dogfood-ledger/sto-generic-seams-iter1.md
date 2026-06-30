# sto-generic-seams — dogfood iter1

**Date:** 2026-06-29
**Branch:** feat/sto-generic-seams (HEAD 697c36e+)
**Persona:** fork-receiver building a Korean STO (Security Token Offering) product who exercises
all 5 tokenized-securities seams composed in a single realistic lifecycle flow.
**Test artifact:** `TokenizedSecuritiesDogfoodE2ETest` (`@Tag("TOKENIZED_SECURITIES")`)
**Run:** `./gradlew testTokenizedSecurities` → GREEN (14 tests total after E2E addition)

## Seams exercised (composition)

| Seam | Invariant exercised |
|------|-------------------|
| ISSUE-LIFECYCLE | DRAFT created; transfer blocked until ISSUED |
| REG/ISSUE | ADMIN-only issuance; issuer seeded with totalUnits |
| HOLDER-AUTHZ | First-claim-wins ownership; uncontrolled-holder → 403 |
| TRANSFER eligibility | Deny-by-default; ADMIN grant enables; ungranted → 422 |
| TRANSFER + ANCHOR | Atomic debit+credit; Σ conserved; anchorRef non-null; reconcile converges |

## Findings

---

### F1 — No read surface for holder ownership state
**Severity:** MEDIUM
**Evidence:** `HolderOwnershipController` exposes only `POST /api/security-tokens/holders/{holderId}/ownership`.
There is no `GET /api/security-tokens/holders/{holderId}/owner` endpoint.
A fork-receiver building an admin dashboard ("who controls this holder?") or investor KYC portal
("is this wallet bound to a verified identity?") has no read surface — must query the DB directly.
**Suggested fix:** Add `GET /api/security-tokens/holders/{holderId}/owner` returning `OwnershipDto`
(200 if claimed, 404 if unclaimed). No new entity needed — `HolderOwnershipRepository.findByHolderId`.
**Status:** open (scope follow-up)
**References:** `HolderOwnershipController.java`, `HolderOwnershipRepository.java`

---

### F2 — No read surface for the eligible-investors list
**Severity:** MEDIUM
**Evidence:** `EligibleInvestorController` exposes only `POST /api/security-tokens/{tokenCode}/eligible-investors`.
There is no `GET /api/security-tokens/{tokenCode}/eligible-investors` to list granted holders.
An STO admin building an investor-management UI cannot display "who is currently eligible to receive
this token" without a direct DB query.
**Suggested fix:** Add `GET /api/security-tokens/{tokenCode}/eligible-investors` returning `List<GrantDto>`.
`EligibleInvestorRepository.findByTokenId` already exists; the controller just needs the read endpoint
wired with `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.
**Status:** open (scope follow-up)
**References:** `EligibleInvestorController.java`, `EligibleInvestorRepository.java`

---

### F3 — `issue()` seeds the issuer holding but does NOT auto-authorize the issuer member
**Severity:** MEDIUM
**Evidence:** `SecurityTokenRegisterService.issue()` calls `register.seedIssuerHolding()` which
creates the `TokenHolding` row for `issuerHolderId`. However `HolderOwnership` is a separate table.
The issuer member must separately call `POST /api/security-tokens/holders/{issuerHolderId}/ownership`
after issuance — otherwise any transfer attempt returns 403 TS_NOT_HOLDER_CONTROLLER even though the
issuer holding was just seeded by the same admin who issued the token.
A fork-receiver would reasonably expect that issuing a token (which declares `issuerHolderId` at
creation time and binds totalUnits to it) implicitly authorizes the creating member to control that
holder. The current design requires a mandatory two-step post-issue sequence that is not enforced or
documented in the API response.
**Suggested fix (option A):** Document this in the `POST /api/security-tokens/{tokenCode}/issue`
response body with an `ownershipClaimRequired: true` field hint.
**Suggested fix (option B):** Allow `issue` to accept an optional `issuerPrincipal` body param and
auto-create the `HolderOwnership` record in the same transaction (requires policy decision by
fork-receiver on whether the admin or the creator should own it).
**Status:** open (scope follow-up)
**References:** `SecurityTokenRegisterService.java:issue()`, `HolderOwnershipService.java`

---

### F4 — `claimOwnership` returns 201 on idempotent re-claim by the same owner
**Severity:** LOW
**Evidence:** `HOLDER-AUTHZ-002` test comment says "re-claim h2 as A → idempotent (200 or 201, no error)"
and the test passes with 201. HTTP convention: 201 Created should indicate a new resource was created;
200 OK should be used for an idempotent replay that returns the existing representation.
A fork-receiver checking the status code to detect "first-time claim vs replay" cannot distinguish them.
**Suggested fix:** Return 200 OK with the existing `OwnershipDto` when the same principal re-claims
an already-owned holder (already owned by caller → 200; first claim → 201; claimed by different principal → 409).
**Status:** open (scope follow-up)
**References:** `HolderOwnershipService.java`, `HolderOwnershipController.java`

---

### F5 — Gate order exposes issuance state to unclaiming callers
**Severity:** LOW
**Evidence:** `SecurityTokenRegisterService.transfer()` checks `IssuanceStatus != ISSUED` (ISSUE-001)
as the outermost gate, BEFORE `holderAuthorization.controls()` (HOLDER-AUTHZ). A caller who has NOT
claimed `fromHolderId` still receives 409 TS_NOT_ISSUED when the token is in DRAFT — confirming to
them that the token exists and its issuance state. In most STO products token existence is semi-public,
so this is LOW rather than HIGH. However the gate order is not documented in the OpenAPI spec or
the controller's Javadoc; a fork-receiver tightening access (private placement, confidential issuance)
would not know to account for this information-disclosure path.
**Suggested fix:** Add a comment in `SecurityTokenRegisterController` and the relevant OpenAPI spec
documenting the deliberate gate order: ISSUED → HOLDER-AUTHZ → eligibility → holding-limit.
**Status:** open (scope follow-up)
**References:** `SecurityTokenRegisterService.java:transfer()` lines 92-98

---

## Composition flow friction summary

| Step | Friction |
|------|---------|
| 1 createToken | Smooth. Request body and 201 response are intuitive. |
| 2 transfer on DRAFT | Smooth. 409 TS_NOT_ISSUED fires before HOLDER-AUTHZ (see F5 for documentation gap). |
| 3 issue (ADMIN) | Smooth. But requires separate claimOwnership after issue (F3). |
| 4 claimOwnership | Smooth but undiscoverable; no confirmation GET endpoint (F1). No guidance in issue response that this step is required (F3). |
| 5 grant eligibility | Smooth. But no list read endpoint to verify grants (F2). |
| 6 transfer | Smooth. Response body includes anchorRef and all transfer fields. |
| 7 reconcile | Smooth. `converged + breaks` shape is clear. |
| negatives | All 3 negative paths returned expected codes with no surprises. |

**Overall verdict:** The 5 seams compose correctly. All gates fire in the documented order.
The correctness invariants (Σ conserved, fail-closed eligibility, 403 on uncontrolled sender) hold.
Friction is ergonomic (F1–F5), not a correctness gap. No HIGH findings blocking completion.
