---
name: ax-plan
description: >
  Tier-1 pre-code planning skill. Reuses the autoplan brainstorm/interview pattern
  (clarify intent before execution) but constrains its sole output to a filled Spec
  Trio plan — a compliance spec YAML, an OpenAPI contract, and a policy manifest —
  plus 1:1 RED @Tag test stubs (one failing test per spec item) so
  spec_item_verification_binding_guard.sh counts every item as bound from the TDD
  RED state. Runs AFTER /ax-scaffold has emitted the empty skeleton and BEFORE dev
  handoff. Never emits implementation code. Invoke with /ax-plan <domain>.
metadata:
  priority: 1
  tier: 1
  axis: root
  docs:
    - "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/"
    - "https://swagger.io/specification/v3/"
    - "https://owasp.org/www-project-application-security-verification-standard/"
  pathPatterns:
    - 'skills/ax-plan/SKILL.md'
    - 'docs/blueprints/**/plan.md'
  bashPatterns:
    - 'bash skills/ax-plan/scripts/emit-red-stubs.sh'
    - 'bash skills/ax-plan/scripts/check-plan-complete.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-plan
    - plan domain
    - fill spec trio
    - plan spec trio
    - red stubs
    - map items to tests
    - pre-code planning
    - brainstorm domain
  intents:
    - plan a new domain before writing code
    - fill the Spec Trio scaffold with real items
    - clarify domain requirements via interview
    - emit 1:1 RED @Tag stubs per spec item
    - bind every spec item to a verification before implementation
    - hand a filled+traced plan off to dev
  entities:
    - Spec Trio
    - compliance spec
    - OpenAPI contract
    - policy manifest
    - RED @Tag stub
    - spec_item_verification_binding_guard
    - domain_spec_trio_guard
    - domain_mode
    - ax-scaffold
    - ax-verify-domain
---

# ax-plan

Tier-1 pre-code planning skill. It is the **fill+map** stage of the domain
lifecycle: `/ax-scaffold` emits the empty Spec Trio skeleton, `/ax-plan` fills that
skeleton with real compliance items, an OpenAPI contract, and a policy manifest —
then maps every item 1:1 to a **RED @Tag test stub** so the catalog's binding guard
counts each item as verified from the TDD RED state. The filled+traced plan is then
handed to dev, who implements until the RED stubs turn green.

ax-plan reuses the **autoplan brainstorm/interview pattern** — surface taste and
scope decisions at a clarify gate before committing to artifacts — but unlike a
free-form planner its output is **constrained to the Spec Trio**. It never produces
prose roadmaps, design docs, or implementation code. The only writable outputs are
the three Spec Trio files, the two RED test classes, the allowlist line, the
per-domain Gradle task, and a thin `docs/blueprints/<domain>/{plan.md,progress.md}`
memory record.

Invoke with: `/ax-plan <domain-name>`

## Use this skill when

- You ran `/ax-scaffold <domain>` and now hold an empty skeleton (placeholder YAML +
  TODO markers) that must be filled before any code is written.
- `domain_spec_trio_guard.sh` or `spec_item_verification_binding_guard.sh` is FAILING
  for a domain because its Spec Trio is empty or its items are unbound.
- `/ax-scaffold` or a dev attempt was **routed here** because no filled+traced Spec
  Trio exists yet (the G006 forcing wire).
- You need to clarify a domain's requirements (mode, external standards, semantic
  complexity, cross-domain composition) before mechanical work starts.

Do **not** use this skill to write entities, services, controllers, or migrations —
that is the dev-handoff phase. ax-plan stops at RED.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Confirm the skeleton exists — `bash skills/ax-plan/scripts/check-plan-complete.sh <domain>` (expect FAIL: empty/unfilled — this is the entry state)
- [ ] Step 2: Brainstorm/interview — clarify domain_mode, external standards, semantic complexity, cross-domain composition (constrained AskUserQuestion set below)
- [ ] Step 3: Outline the Spec Trio — map interview answers to compliance families + item count, contract endpoints, manifest policy values
- [ ] Step 4: Fill `specs/<domain>-l0.yaml` (or `<domain>-frontend-l0.yaml`) — every item carries all 8 mandatory fields + concrete `notes` scenario; declare `domain_mode`
- [ ] Step 5: Fill the contract — `contracts/<domain>-openapi.yaml` (3.0.3) for backend; `contracts/<domain>-ui.yaml` for frontend; operationIds match item refs
- [ ] Step 6: Fill the manifest — `blueprints/<domain>-manifest.yaml` with `when_to_use`/`not_for`/`must_not`/`reject_if`/`source_precedence` + every threshold a `policy_ref` points to
- [ ] Step 7: Register `domain_mode` in `practices/evals/trio_integrity_allowlist.yaml`
- [ ] Step 8: Emit 1:1 RED @Tag stubs — `bash skills/ax-plan/scripts/emit-red-stubs.sh <domain>` (one failing `@Test @Tag("ITEM-ID")` per applicable item + the mandatory ViolationProofTest)
- [ ] Step 9: Register the per-domain Gradle task `test<Domain>` (UPPERCASE domain `@Tag` via `includeTags`)
- [ ] Step 10: Write `docs/blueprints/<domain>/{plan.md,progress.md}` (ADR + 5 binary principles + family table; cold-start record only — no roadmap)
- [ ] Step 11: Verify the PLAN is complete + traced (not green) — `bash skills/ax-plan/scripts/check-plan-complete.sh <domain>` exits 0
- [ ] Step 12: Hand off to dev — RED is the start state; implement until `./gradlew test<Domain>` is green

## Steps detail

### Step 1: Confirm the skeleton entry state
ax-plan does not scaffold. It assumes `/ax-scaffold <domain>` already wrote the empty
Spec Trio (placeholder YAML with TODO markers). Run
`bash skills/ax-plan/scripts/check-plan-complete.sh <domain>`. On a fresh skeleton it
**FAILS** (`PLAN_INCOMPLETE` — files present but unfilled / items unbound). That FAIL
is the signal there is work to do. If the script reports `SKELETON_MISSING`, run
`/ax-scaffold <domain>` first. If it already exits 0, the plan is done — go to dev.

### Step 2: Brainstorm / interview (the autoplan pattern, constrained)
Surface the decisions a free-form planner would, but only the ones that shape the
Spec Trio. Use `AskUserQuestion` (one round, batched) for exactly these axes — do not
expand into open-ended design discussion:

1. **Domain mode** — `full_trio` (backend API + UI) / `backend_only` (API, no UI) /
   `frontend_only` (static viewer, no backend OpenAPI). This decides which Spec Trio
   files are mandatory (see `domain_spec_trio_guard.sh`).
2. **External standards** — which OWASP ASVS chapters, RFC/JEP numbers, CWE IDs, or
   vendor docs the domain must anchor to (drives the spec `standard:` field and every
   rule's `evidence:` block). If a standard has a `practices/upstream/*.snapshot.md`,
   prefer it; otherwise cite RFC/JEP/OWASP with URL + quoted excerpt.
3. **Semantic complexity signals** — does the domain have a state machine? money?
   concurrency / optimistic locking? multi-tenancy? PII/PHI (regulated)? idempotent
   mutation? Each "yes" pulls in a `common/` primitive (see NEW-DOMAIN-CHECKLIST §0)
   and a compliance family.
4. **Cross-domain composition** — does this domain compose existing L4 domains
   (payment + refund, audit-on-write, search index)? Composition becomes contract
   `$ref`s and manifest `source_precedence`, not new code.

Decision principle (borrowed from autoplan): when an answer is borderline, pick the
**tighter scope** — fewer items, one mode, deferred provider integration — and record
the rejected alternative in `plan.md`'s ADR. Tight scope is a virtue, not a gap.

### Step 3: Outline the Spec Trio
Translate the interview into a family → item-count table (the Payment canonical shape:
AUTHZ / IDEMP / STATE / MONEY / PROVIDER / SEC / OBS / RECON for a payment-like
domain). Each family becomes a block of `{DOMAIN}-{FAMILY}-{NNN}` item IDs. The
outline is the contract between interview and artifacts; it lives in `plan.md` Step 10,
but you build it here so the fills in Steps 4–6 are deterministic, not improvised.

### Step 4: Fill the compliance spec
Write `specs/<domain>-l0.yaml` (backend / backend_only) or
`specs/<domain>-frontend-l0.yaml` (frontend_only). Top-level keys: `version`, `scope`,
`domain_mode`, `stack`, `standard`, `items`. **Every item MUST carry all 8 mandatory
fields**: `id`, `chapter`, `requirement`, `test_method`, `verification_type`,
`applicable`, `notes`, `policy_ref`. Hard rules:

- `id` format `{DOMAIN}-{FAMILY}-{NNN}` (e.g. `KEY-AUTHN-001`) — it MUST match the
  `@Tag` the RED stub will carry, exactly. The binding guard's item regex is
  `^[A-Z0-9]+-(?:[A-Z0-9]+-)?[0-9]`; IDs outside it are silently skipped (a trap).
- `verification_type` is commonly `positive | negative | e2e_test`; it is NOT a closed
  3-value enum — the repo also uses `both | review | code_review | integration_test |
  static_analysis | structural`. No guard restricts the set; pick the truthful one.
- `notes` MUST be a concrete scenario with expected HTTP status / behavior
  (e.g. "POST /api/api-keys → 201 includes plaintext `value`; GET → 200 omits it").
  Ambiguous notes force interpretation gaps at dev time — they are a defect here.
- `policy_ref` uses `path#anchor` (e.g. `blueprints/api-key-manifest.yaml#issuance`)
  and points at a real manifest section. Thresholds live in the manifest, never in
  `notes`.
- `domain_mode` MUST appear at top level (or `domain_spec_trio_guard.sh` FAILs with
  "spec does not declare domain_mode").

### Step 5: Fill the API / UI contract
- **backend / full_trio**: `contracts/<domain>-openapi.yaml`, OpenAPI **3.0.3** (not
  2.0). `info`/`servers`/`paths`/`components`. Every `operationId` that a spec item or
  a UI route references MUST exist here. Include `ErrorResponse` and
  `ValidationErrorResponse` schemas (`application/problem+json`, RFC 9457).
- **full_trio (UI side)**: `contracts/<domain>-ui.yaml` — routes with
  `path`/`method`/`backend_operation_id` (non-null, resolving into the OpenAPI) and
  `static_source_ref` empty.
- **frontend_only**: `contracts/<domain>-ui.yaml` only — every route has
  `backend_operation_id: null` AND a non-empty `static_source_ref` to existing files.
- `backend_only`: the contract is optional (`domain_spec_trio_guard.sh` requires only
  spec + manifest for `backend_only`); fill it if the domain has HTTP endpoints.

`backend_operation_id` and `static_source_ref` are **mutually exclusive** — exactly
one is set per route.

### Step 6: Fill the policy manifest
Write `blueprints/<domain>-manifest.yaml`. Model it on `blueprints/api-key-manifest.yaml`:
top-level keys `template_id`, `version`, plus the decision blocks `when_to_use`,
`not_for`, `must_not`, `reject_if`, `source_precedence`, and `testing_baseline` /
`verification_checkpoints`. (No guard enforces an exact key set — match the canonical
manifest rather than inventing ceremony.) Add domain-specific policy blocks (e.g. `rate_limits`,
`state_machine`, `currency_scales`, `retry_policy`) — **every threshold a spec
`policy_ref` anchor names must have a real value here**. The manifest is what makes
the VIOLATION proof possible: a test reads the policy value, and flipping it (e.g.
`max_per_window: 5 → 9999`) must make a test FAIL.

### Step 7: Register the domain mode
Add `<domain>: <mode>` under `domains:` in
`practices/evals/trio_integrity_allowlist.yaml`. Without it,
`trio_integrity_guard.sh` FAILs with `ZERO_SCAN` / `DOMAIN_NOT_IN_ALLOWLIST`. This is
the line `/ax-verify-domain` Step 1 checks.

### Step 8: Emit 1:1 RED @Tag stubs (the core mechanism)
Run `bash skills/ax-plan/scripts/emit-red-stubs.sh <domain>`. It reads
`specs/<domain>-l0.yaml`, and for **every `applicable: true` item** writes one method:

```java
@Test
@Tag("API_KEY")          // class/domain tag — puts the method in test<Domain> (includeTags)
@Tag("KEY-AUTHN-001")    // item-ID tag — binds the spec item (guard counts iid ∈ tags)
void plan_KEY_AUTHN_001_item1() {   // generated name: plan_<id-underscored>_item<idx>
    org.junit.jupiter.api.Assertions.fail(
        "RED: implement KEY-AUTHN-001 — POST returns plaintext once; GET omits it");
}
```

into `backend/src/test/java/com/ax/template/authblueprint/<domain>/<Domain>ComplianceTest.java`,
and it writes the **mandatory** `<Domain>ViolationProofTest` stub (without it,
`l4_domain_reachability_guard` FAILs the build). Both classes carry the UPPERCASE
domain `@Tag` (`test_tag_naming_convention_guard`). The method-level `@Tag("ITEM-ID")`
is what `spec_item_verification_binding_guard.sh` resolves via its implicit
`iid ∈ tags` path — so a *failing* stub still satisfies the binding invariant. RED =
TDD start state; the guard counts the item as **bound**, not as **passing**.

If a spec item is genuinely not yet test-backed (illustrative / backlog), hand-add
`verification: { mechanism: deferred, citation: "<reason>" }` to that item in the spec
yourself — `emit-red-stubs.sh` SKIPS any item that already declares a `verification:`
block, so it will leave that item alone. (The script never mutates the spec; it only
reads it and writes test stubs.) That is the only sanctioned way to leave an item
without a RED test. For `frontend_only` / UI items there is no backend `@Tag` test
path; bind them with `verification: { mechanism: rule, ref: <practices-react-rule> }` —
the mechanism:rule binding used across `specs/*-frontend-l0.yaml` (e.g. audit-log-frontend);
`specs/practices-frontend-l0.yaml` is the canonical `domain_mode: frontend_only` — instead
of a Java stub.

### Step 9: Register the per-domain Gradle task
Append to `backend/build.gradle.kts`:

```kotlin
tasks.register<Test>("test<Domain>") {
    useJUnitPlatform { includeTags("<DOMAIN>") }   // UPPERCASE
    group = "verification"
}
```

After this, `./gradlew test<Domain>` runs the RED stubs and reports RED (expected).
That is the dev's progress meter: RED → GREEN, item by item.

### Step 10: Write the cold-start memory record
Write `docs/blueprints/<domain>/plan.md` and `progress.md`. `plan.md` is the ADR
(Decision / Drivers / Alternatives considered / Why chosen / Consequences /
Follow-ups), the 5 binary-checkable principles, and the family → item-count table —
mirroring the Payment plan. `progress.md` lists each item with a RED/GREEN checkbox.
**Do not** write a phased execution roadmap or implementation code; ax-plan's output
is the Spec Trio + stubs, and a thin record so a cold-start agent can resume.

### Step 11: Verify the plan is complete and traced
Run `bash skills/ax-plan/scripts/check-plan-complete.sh <domain>`. It delegates to the
two promoted hard guards (it never re-implements their logic) plus a fill check:
`domain_spec_trio_guard.sh` (all Trio files present per mode + `domain_mode` declared)
and `spec_item_verification_binding_guard.sh` (every applicable item resolves a
binding). Exit 0 means: **plan complete, fully traced, all RED**. It does **not** run
`./gradlew test<Domain>` — green is dev's job, not the plan's.

### Step 12: Hand off to dev
The handoff artifact is the repo state itself: a filled Spec Trio, RED stubs that
compile and fail, an allowlist line, a Gradle task, and `plan.md`. Dev (or
`/ax-scaffold` fill + implementation) turns each RED green by building the entities,
service, controller, state machine, migration, and `@ExceptionHandler` per
`docs/NEW-DOMAIN-CHECKLIST.md`. Done = `./gradlew test<Domain>` green +
`/ax-verify-domain <domain>` exit 0 + R25 `verify-completion.sh` PASS.

## Bundled scripts
- `skills/ax-plan/scripts/emit-red-stubs.sh` — reads `specs/<domain>-l0.yaml`; writes one failing `@Test @Tag("ITEM-ID")` per applicable item into `<Domain>ComplianceTest.java` + the mandatory `<Domain>ViolationProofTest.java` stub; accepts `<domain>`; exit 0 on success
- `skills/ax-plan/scripts/check-plan-complete.sh` — delegates to `domain_spec_trio_guard.sh` + `spec_item_verification_binding_guard.sh` (no re-implementation) + a fill check; exit 0 iff plan is complete, traced, and all items RED-bound

## Feedback loop
When Step 1 reports `SKELETON_MISSING`: run `/ax-scaffold <domain>` first.
When Step 4 fails the binding guard with an unbound item: confirm the spec `id` matches
the `^[A-Z0-9]+-(?:[A-Z0-9]+-)?[0-9]` regex and that the RED stub's method-level
`@Tag` string is byte-identical to the item `id`.
When Step 7 is skipped: `trio_integrity_guard.sh` FAILs `ZERO_SCAN` — add the
allowlist line.
When `check-plan-complete.sh` reports a Trio gap: fill the file named in the guard's
message for the declared `domain_mode`.
Halt threshold: 3 consecutive failures after fixes → record the blocker in
`practices/DECISIONS.md` and halt.

## Invocation graph
- Calls (Tier-2): none directly; delegates verification to Tier-3 guards via
  `check-plan-complete.sh` (`domain_spec_trio_guard`, `spec_item_verification_binding_guard`)
- Called by (Tier-1): user directly; **routed-to by `/ax-scaffold` / dev entry** when a
  filled+traced Spec Trio is absent (the G006 forcing wire)
- Hands off to: `/ax-scaffold` (fill) + dev implementation → `/ax-verify-domain <domain>`

## The chain
```
/ax-scaffold <domain>     ->  empty Spec Trio skeleton (TODO placeholders, no items, no tests)
        |  handoff state: files present, content empty
        v
/ax-plan <domain>         ->  filled Spec Trio + 1:1 RED @Tag stubs + allowlist + Gradle task + plan.md
        |  handoff state: items bound (RED), check-plan-complete.sh exits 0, ./gradlew test<Domain> is RED
        v
dev handoff               ->  implement entities/service/controller/migration until RED -> GREEN
        |  done state: ./gradlew test<Domain> green + /ax-verify-domain <domain> exit 0 + R25 PASS
```

## Worked example (api-key domain)
1. `/ax-scaffold api-key` → `specs/api-key-l0.yaml` etc. exist with TODO placeholders.
2. Interview: mode = `full_trio`; standard = OWASP ASVS V2.10 + CWE-321 + CWE-200;
   complexity = constant-time compare (no state machine, no money); composition =
   none.
3. Outline: families AUTHN(3) / STORAGE(3) / LIFECYCLE(3) / AUTHZ(3) = 12 items.
4. Fill `specs/api-key-l0.yaml`: `KEY-AUTHN-001` … `KEY-AUTHZ-003`, each with all 8
   fields, concrete `notes`, `policy_ref: blueprints/api-key-manifest.yaml#...`.
5. Fill `contracts/api-key-openapi.yaml` (operationIds `createApiKey`, `getApiKey`,
   `rotateApiKey`, `revokeApiKey`) + `contracts/api-key-ui.yaml`.
6. Fill `blueprints/api-key-manifest.yaml` (`#issuance`, `#authentication`,
   `#storage`, `#lifecycle`).
7. Allowlist: `api-key: full_trio`.
8. `emit-red-stubs.sh api-key` → `ApiKeyComplianceTest` with 12 `@Test @Tag("API_KEY")
   @Tag("KEY-…")` failing methods + `ApiKeyViolationProofTest` stub.
9. Gradle: `tasks.register<Test>("testApiKey") { useJUnitPlatform { includeTags("API_KEY") } }`.
10. `docs/blueprints/api-key/plan.md` ADR + 5 principles + family table.
11. `check-plan-complete.sh api-key` → exit 0 (12 items bound, all RED).
12. Dev implements until `./gradlew testApiKey` is green.

## Acceptance (binary)
```bash
bash skills/ax-plan/scripts/check-plan-complete.sh api-key
# Expected: exit 0 — Spec Trio filled for declared domain_mode AND every applicable
#           item resolves a verification binding (RED @Tag stubs counted as bound).
#           Green-ness of ./gradlew testApiKey is NOT asserted here — that is dev's gate.
```
