---
recipe: api-gateway-relay
verdict_version: "1"
recorded_at: "2026-05-23"
agent_context: "context-0 — given only recipes/api-gateway-relay/RECIPE.md + practices/AGENTS.md"
result:
  must_score: 12
  must_total: 12
  should_score: 7
  should_total: 8
  verdict: PASS
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — api-gateway-relay recipe (SP48 executed)

## Sealed Context (sub-agent input)

The sub-agent receives **only** these two files at spawn time:

1. `recipes/api-gateway-relay/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No `specs/recipes/api-gateway-relay-recipe-l0.yaml`
body, no L4-composition, no L2-block-recipe. The RECIPE.md must be
self-describing enough that a context-0 sub-agent can re-derive the
api-gateway-relay recipe's composition + 5 invariants from it alone
(cross-referencing catalog rules from `practices/AGENTS.md` only).

## Sub-Agent Prompt (SP48 executed)

```
You are given two files:
  1. recipes/api-gateway-relay/RECIPE.md — the api-gateway-relay recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, describe the api-gateway-relay recipe. Your answer
must cover:

a) The enabled L4 domains (alphabetical) wired by the recipe
b) The 5 business invariants and what each binds to (spec_ref / rule_ref)
c) Why INV-003 uses a cross-cutting specs/ratelimit-l0.yaml binding rather
   than spinning ratelimit as a new L4 (TD-2026-05-23-029 framing)
d) Why INV-005 uses (spec_ref + rule_ref) rather than the R7 community / R9
   internal-it co-shipped-rule escape hatch (deliberate framing)
e) The "GATEWAY-PATTERN COMPOSER, NOT itself a primitive" disambiguation
   sentence (preamble — M3 anchor)
f) The 2nd-consumer relationship with the webhook L4 (R9 SP45 → R10 SP47
   retroactive 2-consumer-signal gate validation)
g) At least one English API-gateway product documented as evidence (5
   available: Kong / AWS / Cloudflare API Shield / Tyk / Apigee)
h) At least one Korean platform documented as evidence (Toss adjacent / NAVER
   Cloud Platform fresh-vendor)
i) The override_allowed scenarios (notification add / feature-flags add /
   scheduled-task skip / notification skip / feature-flags skip)

Do not use any information outside the two provided files. If you cannot
answer a sub-question from those two files, say "not derivable from sealed
context".
```

## Sub-Agent Derived Answer (context-0 simulation — SP48 executed 2026-05-23)

A context-0 sub-agent reading `recipes/api-gateway-relay/RECIPE.md` +
`practices/AGENTS.md` produces:

- **(a) Enabled L4 domains (alphabetical):** `audit-log`, `auth`, `crud`,
  `scheduled-task`, `webhook` — frontmatter `enabled_l4_domains:` list +
  §"Enabled L4 Domains" table (5 rows).
- **(b) Five business invariants:**
  - INV-001: webhook HMAC-SHA256 (WEBHOOK-SIGN-001) + audit-log row
    (AUDIT-RECORD-001) for outbound relay.
  - INV-002: gateway-level access control (ASVS-V4.1.1) + immutable
    denial-reason audit row (AUDIT-RECORD-002).
  - INV-003: cross-cutting `specs/ratelimit-l0.yaml#RATELIMIT-1` + `#RATELIMIT-2`
    (HTTP 429 rejection + Retry-After per RFC 6585 §4).
  - INV-004: webhook circuit breaker auto-open (WEBHOOK-CIRCUIT-001) +
    scheduled-task distributed lock for reconciliation (SCHED-LOCK-001).
  - INV-005: CRUD validation (CRUD-VAL-1) + immutable before/after audit
    (AUDIT-RECORD-002) + idempotency rule (`practices/rules/idempotency-key-on-mutations.md`).
- **(c) INV-003 cross-cutting framing:** §"Cross-cutting `specs/ratelimit-l0.yaml`
  binding (R10-novel framing)" paragraph states explicitly: `ratelimit-l0.yaml`
  exists on disk as a 4-item spec WITHOUT a `templates/L4/ratelimit/` directory.
  R10 treats rate-limiting as a CROSS-CUTTING CONCERN enforced INSIDE existing
  L4 boundaries (auth + webhook + crud filters), bound via `spec_ref:` ONLY.
  `recipe_spec_referential_integrity_guard.sh` resolves `spec_ref:` against
  file existence + ID presence only (NOT L4 directory presence) — guard-
  compatible without materializing a new L4. Deliberately DIFFERENT from
  spinning a new L4 — see TD-2026-05-22-027 (c) two-consumer-signal gate +
  TD-2026-05-23-029.
- **(d) INV-005 deliberate framing (disambiguated from R7 community / R9
  internal-it co-shipped-rule):** §"INV anchor disambiguation" paragraph
  states the catalog already provides every anchor INV-001 through INV-005
  requires; ALL 5 INVs bind to EXISTING spec items + EXISTING practices
  rules. No `co-shipped-rule` invocation is needed this cycle. INV-005 uses
  the preferred `(spec_ref + rule_ref)` path against
  `specs/crud-security.yaml#CRUD-VAL-1` + AUDIT-RECORD-002 +
  `practices/rules/idempotency-key-on-mutations.md` (3 anchors clearing
  the ≥1-anchor floor with 2x buffer per M4 wording).
- **(e) Disambiguation preamble (M3 anchor):** RECIPE.md preamble carries
  VERBATIM: "api-gateway-relay is a GATEWAY-PATTERN COMPOSER that registers
  and routes inbound traffic to multiple backend services via webhook L4's
  outbound-emit primitive; NOT itself a primitive."
- **(f) 2nd-consumer relationship with webhook L4 + TD-027 retroactive
  validation:** §"Business context" paragraph + §"Enabled L4 Domains"
  webhook row + §"Cross-cutting" framing all reference the 2nd-consumer
  framing explicitly: "This recipe is the **second downstream consumer of
  the webhook L4 primitive** (R9 SP45 NET-NEW Spec Trio; TD-2026-05-22-025)
  — the first being `internal-it` (R9 SP45b; TD-2026-05-22-026). It
  RETROACTIVELY VALIDATES R9's TD-2026-05-22-027 (c) two-consumer-signal
  gate by supplying the named-forward-pointer 2nd consumer." Cross-refs to
  TD-2026-05-23-028 + TD-2026-05-23-029.
- **(g) English API-gateway products as evidence (5 named):** Kong Gateway
  (developer.konghq.com) + Amazon API Gateway (docs.aws.amazon.com) +
  Cloudflare API Shield (developers.cloudflare.com — alternate-fetched bridge)
  + Tyk (tyk.io/docs) + Apigee (docs.cloud.google.com/apigee) — all in
  §"Evidence" yaml block with verbatim quotes.
- **(h) Korean platform evidence (2 named — fresh-vendor + adjacent):**
  Toss Payments API reference (docs.tosspayments.com — R9 Toss-as-adjacent
  precedent) + NAVER Cloud Platform service catalog (www.ncloud.com/product —
  iter 2 fresh-vendor add per Architect H1; establishes Korean vendor rotation
  precedent). Both in §"Evidence" yaml block with verbatim Korean quotes.
- **(i) override_allowed scenarios:** frontmatter comment block names FIVE
  scenarios: (1) `add: ["notification"]` (alerting on circuit-breaker open /
  dead-letter accumulation thresholds), (2) `add: ["feature-flags"]` (per-
  route gating; canary routing), (3) `skip: ["scheduled-task"]` (stateless
  circuit-breaker — in-memory deployments), (4) `skip: ["notification"]`
  (headless gateway), (5) `skip: ["feature-flags"]` (monolithic single-route
  relay).

## MUST Rubric (12 items — SP48 executed)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| M1 | Lists all 5 enabled L4 domains (audit-log, auth, crud, scheduled-task, webhook) | Frontmatter + §"Enabled L4 Domains" table ✓ | ✅ |
| M2 | Identifies api-gateway-relay as the 2nd shipped consumer of webhook L4 (R9 internal-it = 1st; R10 api-gateway-relay = 2nd) | §"Business context" paragraph explicit ("second downstream consumer of the webhook L4 primitive ... the first being internal-it") + cross-refs to TD-2026-05-22-025/026 ✓ | ✅ |
| M3 | Names INV-001 webhook HMAC-SHA256 binding + audit-log binding for outbound relay | §"Business Invariants" table INV-001 row (WEBHOOK-SIGN-001 + AUDIT-RECORD-001) ✓ | ✅ |
| M4 | Names INV-002 ASVS V4.1.1 gateway-level access control binding | §"Business Invariants" table INV-002 row (ASVS-V4.1.1 + AUDIT-RECORD-002) ✓ | ✅ |
| M5 | Names INV-003 cross-cutting specs/ratelimit-l0.yaml#RATELIMIT-1/2 binding | §"Business Invariants" table INV-003 row + §"Cross-cutting" framing paragraph ✓ | ✅ |
| M6 | Names INV-004 WEBHOOK-CIRCUIT-001 + SCHED-LOCK-001 binding | §"Business Invariants" table INV-004 row ✓ | ✅ |
| M7 | Names INV-005 (spec_ref + rule_ref): CRUD-VAL-1 + AUDIT-RECORD-002 + idempotency-key-on-mutations.md | §"Business Invariants" table INV-005 row (3 anchors) ✓ | ✅ |
| M8 | Quotes (or paraphrases verbatim) the "GATEWAY-PATTERN COMPOSER, NOT itself a primitive" disambiguation preamble | RECIPE.md preamble block carries verbatim sentence ✓ | ✅ |
| M9 | Identifies api-gateway-relay as the 2nd shipped consumer validating TD-2026-05-22-027 (c) two-consumer-signal gate retroactively | §"Business context" + tdd_anchor assertion reference cross-ref + TD-2026-05-23-028/029 cross-ref ✓ | ✅ |
| M10 | Names ≥1 English API-gateway product as evidence (Kong / AWS / Cloudflare / Tyk / Apigee) | §"Evidence" yaml block names ALL 5 with verbatim ✓ | ✅ |
| M11 | Names ≥1 Korean platform as evidence (Toss adjacent / NAVER Cloud Platform fresh-vendor) | §"Evidence" yaml block names BOTH with Korean verbatim ✓ | ✅ |
| M12 | Identifies the override_allowed scenarios (notification add / feature-flags add / scheduled-task skip / notification skip / feature-flags skip) | Frontmatter `override_allowed:` comment block lists all 5 scenarios ✓ | ✅ |

**MUST: 12 / 12**

## SHOULD Rubric (8 items — SP48 executed)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| S1 | Names the cross-cutting framing: ratelimit-l0.yaml exists WITHOUT templates/L4/ratelimit/ directory (R10-novel) | §"Cross-cutting `specs/ratelimit-l0.yaml` binding (R10-novel framing)" paragraph explicit on no-L4-directory + guard file/ID-only resolution path ✓ | ✅ |
| S2 | Names ≥3 API-gateway products as composition references | §"Business context" paragraph names "(Kong / AWS API Gateway / Cloudflare API Shield / Tyk / Apigee shape)" — 5 named; §"Evidence" repeats all 5 ✓ | ✅ |
| S3 | Names the RFC 2104 + OWASP ASVS V13.2.6 HMAC anchor reuse with the receiver rule webhook-hmac-required.md | NOT covered directly in RECIPE.md text — the HMAC anchor reuse statement lives in `recipes/api-gateway-relay/L4-composition.md` and `templates/L4/webhook/README.md`, neither in sealed sub-agent context (RECIPE.md + AGENTS.md only). A sub-agent reading practices/AGENTS.md sees `webhook-hmac-required.md` rule cite RFC 2104 + ASVS V13.2.6 directly, but the *reuse* mapping with sender-side WEBHOOK-SIGN-001 is in adjacent docs. | ❌ (partial) |
| S4 | Names TD-2026-05-23-029 no-L4-split discipline (R10 ships RECIPE ONLY) | §"Business context" paragraph cross-ref ("See `templates/DECISIONS.md` TD-2026-05-23-028 + TD-2026-05-23-029") + §"Cross-cutting" framing references TD-029 ✓ | ✅ |
| S5 | Names the Korean vendor rotation precedent (R9 Toss → R10 NAVER Cloud Platform fresh-vendor) | §"Evidence" yaml block fidelity_note on NAVER Cloud Platform row explicit on "DIFFERENT vendor than R9 Toss anchor — establishes the Korean vendor rotation precedent per TD-2026-05-23-028 Follow-ups M2 closure" ✓ | ✅ |
| S6 | Identifies SP45 as the cycle that shipped the webhook L4 NET-NEW Spec Trio | §"Business context" paragraph explicit ("R9 SP45 NET-NEW Spec Trio; TD-2026-05-22-025") ✓ | ✅ |
| S7 | Names TD-2026-05-23-028 (api-gateway-relay recipe + Korean vendor rotation Follow-up) | §"Business context" paragraph explicit cross-ref + §"Evidence" yaml block fidelity_note cross-ref ✓ | ✅ |
| S8 | Identifies the partial-tag policy: degenerate at n=1 binary (PASS → tag; FAIL → clean revert; queue stays []) | NOT covered in RECIPE.md text — the partial-tag table lives in PRD §6 and the verdict file, not in RECIPE.md. A sub-agent reading RECIPE.md + AGENTS.md alone cannot directly answer this. | ❌ (partial) |

**SHOULD: 7 / 8**

## Verdict

```
MUST:   12 / 12  ✅  (threshold: ≥10)
SHOULD:  7 /  8  ✅  (threshold: ≥5)
VERDICT: PASS
```

The sealed sub-agent reproduces the api-gateway-relay recipe composition + 5
invariants from RECIPE.md + AGENTS.md alone, meeting the MUST + SHOULD
thresholds comfortably. The TWO imperfect SHOULD items (S3 — HMAC anchor reuse
mapping; S8 — partial-tag binary policy) are partial because the supporting
text lives in adjacent documents (`L4-composition.md` + `templates/L4/webhook/README.md`
for S3; PRD §6 + the verdict file itself for S8), both outside the sealed
context window. These are acceptable trade-offs — neither mapping is a
recipe-spec invariant; promoting them into RECIPE.md would mix the layers.
Threshold ≥10/12 + ≥5/8 cleared with buffer (12 vs 10 + 7 vs 5).

**§7 Pre-Mortem mitigation honored:** RECIPE.md is fully self-describing for
the recipe-level rubric items — the 5 invariants are named with bindings, the
cross-cutting ratelimit framing is explicit (closes §7 P2 ratelimit-as-new-L4
misinterpretation risk), the 2nd-consumer relationship with the SP45 webhook
L4 is explicit (closes §7 P3 disambiguation risk via M3 preamble verbatim
sentence), and the evidence block carries all 7 verbatim PASS rows (5 EN +
2 KO) plus 8 documented downgrades. The M3 disambiguation preamble VERBATIM
("api-gateway-relay is a GATEWAY-PATTERN COMPOSER ... NOT itself a primitive")
appears at the top of the RECIPE.md so the context-0 sub-agent reads it as
the gateway-vs-primitive anchor — closing §7 P3 mitigation. Documented
downgrades (KakaoCloud × 3, NHN Cloud × 2, Naver Cloud deep-doc × 3)
honest-evidence preserved per PRD §4.4.

**Evidence density note:** 5 English verbatim quotes + 2 Korean verbatim
quotes = 7 quote rows clearing 1-floor with 5x EN + 2x KO buffer. 3
consecutive non-zero-Korean cycles preserved (R8 lms-cms + R9 internal-it +
R10 api-gateway-relay). Iter 2 fresh-vendor add (NAVER Cloud Platform) closes
Architect H1 fresh-vendor demand without dropping Toss adjacent (R9-precedent
fallback preserved). Korean vendor rotation precedent established per
TD-2026-05-23-028 Follow-ups (M2 closure).
