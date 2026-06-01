---
title: Catalog utilities MUST be promoted to a shared package on the third adoption — or carry an explicit deferral with expiry
impact: MEDIUM
impactDescription: "Inline-duplicated utilities drift across adopters. Each fix to the canonical version must be hand-mirrored N times, and divergence is a constant audit liability. The third adoption is the cheapest moment to lift; later lifts touch more call sites and accumulate more drift."
tags:
  - catalog-meta
  - shared-utility
  - rule-of-three
  - dry
  - refactor-discipline
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/common/AuditPiiHelper.java"
  pattern: "AuditPiiHelper was inline EmailPiiHelper until 7 modules adopted it; R67 lifted it to common/ once the rule-of-three threshold was satisfied. Inline-duplicate copies are gone; one canonical version, every adopter imports it."
upstream:
  - "https://www.oreilly.com/library/view/the-pragmatic-programmer/020161622X/"
  - "https://abseil.io/resources/swe-book"
evidence:
  - source_type: external
    citation: "Wikipedia — Don't repeat yourself (lead section), stating the DRY principle as formulated by Hunt & Thomas in The Pragmatic Programmer ('The Evils of Duplication'): 'Every piece of knowledge must have a single, unambiguous, authoritative representation within a system.' The rule of three (extract on the third copy) is the practical operationalization."
    url: "https://en.wikipedia.org/wiki/Don%27t_repeat_yourself"
    quote: "Every piece of knowledge must have a single, unambiguous, authoritative representation within a system."
    quoted_at: "2026-05-26"
---

## Catalog utilities MUST be promoted to a shared package on the third adoption — or carry an explicit deferral with expiry

**Impact: MEDIUM — DRY drift is a slow leak that compounds across the catalog**

The catalog is composed of many domain modules that occasionally need the
same utility helper — a PII hasher, a JSON ProblemDetail parser, a string
sanitizer, a cron expression validator. The first two modules to need a
given helper typically duplicate it inline because the cost of lifting
(new package, new test home, import surface) is higher than the cost of
the second copy. **By the third adoption that calculus inverts**: the
helper has proven its general-purpose shape (three independent modules
converged on the same interface), and each further inline copy starts
accumulating its own divergence (the email-outbox copy gets a PII regex
update; the activity-feed copy stays on the old version; ops can no
longer reason about which audit log has the latest scrubber).

R67 in the ax-template session lifted `EmailPiiHelper` →
`com.ax.template.authblueprint.common.AuditPiiHelper` after seven backend
modules had adopted it (the threshold was crossed at module 3 — R63's
multi-module sweep — but the lift itself was deferred two commits). The
threshold-then-defer pattern is fine **only when the deferral is
explicit**, with a recorded expiry. Silent deferral is just permanent
duplication.

**Incorrect — third adopter copies the helper inline without lifting:**

```java
// Module C — third adopter
// Copies the same recipientHash() function inline because "we already
// have two copies, what's one more?" — the helper is now in three
// packages with no canonical source of truth. The next regex fix
// applies to one copy; the others drift silently.
public final class ModuleCPiiHelper {
    public static String recipientHash(String email) {
        if (email == null || email.isBlank()) return "(none)";
        // ... duplicated SHA-256 truncate logic ...
    }
}
```

**Correct — third adopter triggers the lift to a shared package:**

```java
// Step 1: create the shared package (or use an existing one).
// New file: backend/src/main/java/com/ax/.../common/AuditPiiHelper.java
package com.ax.template.authblueprint.common;
public final class AuditPiiHelper {
    public static String piiHash(String value) {
        // canonical implementation lives here
    }
}

// Step 2: every existing caller (including modules A and B that had
// inline copies) imports from the new location. The inline copies
// are deleted in the SAME commit so there is no transition window
// where divergence is possible.

// Module A:
import com.ax.template.authblueprint.common.AuditPiiHelper;
// ... AuditPiiHelper.piiHash(email) ...

// Module B:
import com.ax.template.authblueprint.common.AuditPiiHelper;
// ... AuditPiiHelper.piiHash(userId) ...

// Module C (the third adopter — drove the lift):
import com.ax.template.authblueprint.common.AuditPiiHelper;
// ... AuditPiiHelper.piiHash(phone) ...
```

Reference: [Hunt & Thomas — The Pragmatic Programmer (2nd ed), The Evils of Duplication](https://www.oreilly.com/library/view/the-pragmatic-programmer/020161622X/)
Reference: [Software Engineering at Google — Code Review chapter](https://abseil.io/resources/swe-book)

## When to defer the lift (explicit deferral discipline)

Sometimes the third adoption arrives before the canonical shared package
exists. Lifting at that moment means choosing the package location, the
class name, the public method set — decisions worth two adopters of
context but maybe not enough at three. **Deferral is allowed**, with
three required disciplines:

1. **Record the deferral in the commit message of the third adoption.**
   "Helper X is now in three modules; deferring the lift to package
   common/Y because <reason>. Lift trigger: <fourth adoption | 2026-Q3
   refactor sprint | <named owner>>."
2. **Set a concrete expiry.** Either a date or a triggering event. "Lift
   on the fourth adoption" is the default trigger. "Lift in Q3 2026" is
   acceptable if the upstream package design is contested.
3. **Don't defer twice.** If the helper reaches module 4 without lifting,
   the lift is now overdue. The next adoption MUST do the lift; further
   deferral makes the rule meaningless.

## How to apply

When opening a PR that adds a third call site for an inline-duplicated
helper:

```text
adopter_count = git grep -l "<helperFunctionName>" -- '**/*.java' '**/*.ts' | wc -l

if adopter_count >= 3:
  if shared package exists:
    REQUIRE: same commit moves all inline copies to imports;
             delete the inline duplicates
  else:
    REQUIRE: same commit creates the shared package, lifts all copies
    OR: explicit deferral in commit message with expiry trigger
```

## Anti-patterns

- "We'll lift it later when we have time" — there is no future time when
  this is cheaper; defer with concrete expiry or do it now.
- "Copy is fine; the inline version is small" — small inline copies are
  the worst because they look harmless until a security fix needs to be
  applied to all of them. The PII regex deny-list in
  `templates/L0/fork-receiver-kit/parse-error.ts#sanitizeStoredError` was
  exactly this case until R63 lifted.
- "Different packages, different concerns, the duplication is intentional"
  — sometimes true (e.g. a logger named for the domain), but the helper
  under review has zero domain coupling. Domain-coupled helpers stay in
  their domain package; domain-neutral helpers lift to common.
- "Lift means a breaking change for fork-receivers" — ax-template is a
  source-of-truth catalog, not a published library. Fork-receivers
  receive the post-lift state; no semver to honor.
