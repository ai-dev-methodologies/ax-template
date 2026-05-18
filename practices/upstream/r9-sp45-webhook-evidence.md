# R9 SP45 — Webhook L4 external evidence snapshot

**Fetched:** 2026-05-22
**Purpose:** anchor `templates/DECISIONS.md` TD-2026-05-22-025 (webhook L4
NET-NEW Spec Trio) with verbatim external evidence. Three quotes captured
below (2 vendor + 1 RFC for HMAC anchor reuse); all URLs returned HTTP 200 OK
on 2026-05-22 and the quoted substrings appear verbatim in the rendered page
text per the PRD §4.4 evidence ledger.

These quotes anchor `templates/L4/webhook/README.md`, the manifest, and the
ADR; they are not registered in `practices/upstream/_MANIFEST.yaml` (this file
is a per-ADR evidence ledger, not a `.snapshot.md` time-decay-guarded
snapshot).

---

## Quote 1 — GitHub Webhooks

- **URL:** https://docs.github.com/en/webhooks
- **Fetched at:** 2026-05-22
- **HTTP status:** 200 OK
- **Verbatim quotes:**

> Webhooks provide a way for notifications to be delivered to an external web
> server whenever certain events occur on GitHub.

> You can create webhooks to subscribe to specific events that occur on
> GitHub.

- **Relevance:** GitHub is a canonical webhook platform; the verbatim
  describes the exact semantic the webhook L4 domain encodes (outbound
  delivery to an external endpoint on event occurrence). Subscription
  language directly anchors `WEBHOOK-EMIT-001` (register endpoint + event
  filter) and `WEBHOOK-EMIT-002` (fan-out on event match).

---

## Quote 2 — Stripe Webhooks

- **URL (canonical, post-redirect):** https://docs.stripe.com/webhooks
- **Redirect captured:** `https://stripe.com/docs/webhooks` (301) → above
- **Fetched at:** 2026-05-22
- **HTTP status:** 200 OK
- **Verbatim quotes:**

> After you register a webhook endpoint, Stripe can push real-time event data
> to your application's webhook endpoint when events happen in your Stripe
> account.

> Stripe attempts to deliver events to your destination for up to three days
> with an exponential back off in live mode.

> Stripe signs every webhook event by including a signature in the
> Stripe-Signature header.

- **Relevance:** Stripe is a second canonical webhook platform and uniquely
  attests **all three** of the SP45 deliverable's most-distinct invariants:
  (1) endpoint registration (`WEBHOOK-EMIT-001`); (2) exponential backoff
  retry (`WEBHOOK-RETRY-001` — Stripe's 3-day window is longer than this
  catalog's 5-attempt window but the shape — `exponential back off in live
  mode` — is identical); (3) HMAC signature header (`WEBHOOK-SIGN-001` —
  Stripe uses `Stripe-Signature`, this catalog uses `X-Webhook-Signature`,
  both convey the same `sha256=<hex>` format with a per-endpoint signing
  secret).

---

## Quote 3 — RFC 2104 (HMAC anchor reuse — Codex Critic INFORMATIONAL closure)

- **URL:** https://www.rfc-editor.org/rfc/rfc2104
- **Fetched at:** 2026-05-22
- **HTTP status:** 200 OK (canonical IETF document)
- **Verbatim quote (RFC 2104 §1 — Introduction):**

> This document describes HMAC, a mechanism for message authentication using
> cryptographic hash functions. HMAC can be used with any iterative
> cryptographic hash function, e.g., MD5, SHA-1, in combination with a secret
> shared key.

- **Relevance:** RFC 2104 is the canonical specification of the HMAC
  construction; HMAC-SHA256 (SP45 `WEBHOOK-SIGN-001` + `WEBHOOK-SIGN-002`) is
  HMAC instantiated with SHA-256 as the iterative hash. **Anchor reuse
  statement:** the sender axis (this L4 + spec + manifest) and the receiver
  axis (`practices/rules/webhook-hmac-required.md` + `specs/spring-practices-
  l0.yaml#PRACTICES-INTEG-001`) share this same RFC 2104 anchor — sender
  computes the MAC over `timestamp + "." + body`, receiver verifies the same
  MAC using `MessageDigest.isEqual` for constant-time comparison. No new
  cryptographic primitive is introduced by SP45. The receiver rule's
  existing OWASP ASVS V13.2.6 citation likewise applies to the sender axis
  for payload authenticity (`templates/DECISIONS.md` TD-2026-05-22-025 HMAC
  anchor reuse bullet records this explicitly).

---

## Notes

- These three quotes satisfy the R9 SP45 evidence-density floor (≥1 external
  verbatim per deliverable; 2 vendor + 1 RFC captured for explicit
  cryptographic-anchor traceability per Codex Critic INFORMATIONAL closure).
- The PRD §4.4 evidence ledger documents the full fetch result table for R9
  including the 9 downgrades (Atlassian Cloud × 3 truncation + ServiceNow × 4
  hosts + PagerDuty developer × 3 empty-body) and 5 redirect / alt-host
  captures.
- SP45b internal-it recipe carries its own evidence snapshot at
  `practices/upstream/r9-sp45b-internal-it-evidence.md` (Jira + PagerDuty +
  Toss + Naver Works verbatim).
- Korean engineering blog evidence: SP45 itself ships 0 Korean verbatim (the
  Toss webhook + Naver Works Bot API verbatim sit in the SP45b internal-it
  evidence snapshot since they consume the webhook primitive, not introduce
  it).
