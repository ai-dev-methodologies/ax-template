# DECISIONS — rule provenance + acceptance trail

> Every rule in `practices/rules/*.md` and every rejected candidate is recorded here. The
> file is the audit trail for "why was this rule added / refused / modified?". Without
> this trail the catalog is just opinion.

## How to read an entry

```
## <RULE_ID or candidate name>
- Status: ACCEPT | REJECT | DEFERRED | SUPERSEDED
- Date: YYYY-MM-DD
- Maintainer: <name>
- Evidence: see rule frontmatter `evidence:` (or "none" for REJECT entries)
- Rationale: 1-3 sentences on *why this passes provenance* (or *why not*)
- Alternatives considered: short list + why each lost
- Re-evaluation trigger: concrete event that would force a revisit
```

## Acceptance criteria (any maintainer applying ACCEPT must verify)

1. Rule has `evidence:` with ≥1 entry that either:
   - cites a `_MANIFEST.yaml` snapshot section + quote, OR
   - is an external citation (RFC / JEP / vendor reference / peer-reviewed paper)
2. The quote / citation is reachable today (not stale or 404).
3. The rule is not pure stylistic preference dressed as "best practice".
4. At least one alternative was considered and rejected with rationale.

If any criterion fails, mark `Status: DEFERRED` (or REJECT with rationale) — do not ACCEPT.

---

# Rules — ACCEPTED

> Entries below are placeholders to be filled out during the upcoming retrofit pass.
> Each rule listed here exists in `practices/rules/*.md`; the retrofit will populate the
> `Rationale / Alternatives / Re-evaluation trigger` fields and ensure the rule's own
> `evidence:` block matches what's recorded here.

## PRACTICES-PERS-001 — persistence-no-n-plus-1
- Status: ACCEPT (provisional — pending evidence retrofit)
- Date: 2026-05-15
- Evidence: spring-boot-3.5 snapshot + Spring Data JPA reference
- Rationale: (to be filled — JPA fetching docs explicitly identify N+1 + JOIN FETCH remedy)
- Alternatives considered: @EntityGraph (annotation-driven); pagination-only (does not solve depth)
- Re-evaluation trigger: Spring Data JPA major release changing default fetch semantics

## PRACTICES-TX-001 — transaction-no-self-invocation
- Status: ACCEPT (provisional)
- Evidence: Spring Framework reference §Declarative transactions
- Alternatives considered: AspectJ load-time weaving (heavier; project-wide); manual `TransactionTemplate` (verbose)
- Re-evaluation trigger: Spring AOP moving away from proxy-based advice

## PRACTICES-VAL-001 — validation-mass-assignment-guard
- Status: ACCEPT (provisional)
- Evidence: OWASP Mass Assignment Cheat Sheet + Spring MVC `@ModelAttribute` docs
- Alternatives considered: @JsonView (partial, fragile); allow-list serializers (heavier)
- Re-evaluation trigger: Spring Boot 4 binding model change

## PRACTICES-TEST-001 — testing-restassured-blackbox
- Status: ACCEPT (provisional)
- Evidence: rest-assured.io + Spring Boot Testing reference
- Alternatives considered: MockMvc (tight coupling); WebTestClient (good but reactive-leaning)
- Re-evaluation trigger: rest-assured archived OR Spring Boot deprecates @LocalServerPort

## PRACTICES-CORE-001 — core-constructor-injection
- Status: ACCEPT (provisional)
- Evidence: Spring Framework reference §Constructor-based dependency injection
- Alternatives considered: setter injection (mutability); field injection (reflection-only mocks)
- Re-evaluation trigger: Spring Framework 7 introducing first-class field injection ergonomics

## PRACTICES-CORE-002 — core-aop-proxy-no-final
- Status: ACCEPT (provisional)
- Evidence: Spring Framework reference §Proxying mechanisms (CGLIB constraints)
- Alternatives considered: AspectJ LTW (avoids the restriction at the cost of complexity)
- Re-evaluation trigger: Spring's default proxy backend changing (e.g. native image moving to LTW)

## PRACTICES-CORE-003 — core-singleton-no-mutable-state
- Status: ACCEPT (provisional)
- Evidence: Spring Framework reference §Bean scopes + JCIP "Java Concurrency in Practice"
- Alternatives considered: @Scope("prototype") (heavier, hides shared state); explicit synchronization (verbose)
- Re-evaluation trigger: Project Loom virtual-thread idioms shifting the default reasoning

---

# Rules — REJECTED / DEFERRED candidates (P2-D)

> Every controversial-rule candidate has a record here so the same debate doesn't restart
> from scratch in six months.

## quality-records-over-lombok — REJECTED
- Date: 2026-05-16 (was DEFERRED 2026-05-15)
- Decision: REJECT (closes the debate; not on the candidate list)
- Evidence considered: none authoritative. JEP 395 (records) and Lombok project docs cover overlapping but distinct surface; neither claims the other should be abandoned.
- Rationale: Style preference, not a falsifiable best practice. Records cover the immutable-value-object case but not `@Builder`, `@With`, or `@SneakyThrows`-style sugar. Without a clear external "사실상의 표준" deprecating one in favor of the other, accepting this rule violates the provenance criterion. Additionally, mandating it would cause portability fixtures that legitimately use Lombok (e.g. spring-petclinic forks) to false-fail.
- Alternatives considered: blanket rule (rejected — taste dressed as standard); ACCEPT with allow-list of safe Lombok annotations (rejected — same provenance gap); ACCEPT only in `ax-template` reference impl (rejected — ax-template already uses records by convention so the rule would be vacuous).
- Re-evaluation trigger: a JEP or OpenJDK communication that explicitly recommends migration, OR a documented community-wide consensus (e.g. Spring style guide adopting it). Until that exists, do not relitigate.

## arch-hexagonal-mandatory — REJECTED (new arch rules); arch category retained at current 3 rules
- Date: 2026-05-16 (was DEFERRED 2026-05-15)
- Decision: REJECT *new* hexagonal/modulith-style rules. The existing `arch-no-cyclic-package`, `arch-layer-boundary`, and `arch-repositories-extend-jpa` rules are universal and stay.
- Evidence considered: Alistair Cockburn's hexagonal architecture paper; Spring Modulith reference; the universal-vs-style-specific distinction.
- Rationale: The currently shipped arch rules (cyclic package, layer boundary, JPA repository convention) are universal — they hold under layered, hexagonal, Modulith, and clean architecture alike. Mandating a *specific* architectural style (Hexagonal port/adapter package layout, or Modulith module boundaries) would impose ax-template's taste on consumers of the template and would cause portability fixtures using traditional layered structure to false-fail. The arch category is therefore **closed at 3 rules** — no new style-specific rules accepted.
- Alternatives considered: Hexagonal-mandatory (rejected — imposes taste, breaks portability); Spring Modulith adoption (rejected — needs new dependency + vacuous on non-Modulith fixtures); leave the category open-ended (rejected — invites style debates per-PR).
- Re-evaluation trigger: a Spring-blessed architecture style guide that picks one and only one architecture for new projects.

## native-graalvm-required — REJECTED (category-level rejection)
- Date: 2026-05-16 (was DEFERRED 2026-05-15)
- Decision: REJECT the entire `native-` category. Catalog narrows from 22 → **21 categories**.
- Evidence considered: Spring Boot Native Image reference; GraalVM project documentation; the build-time cost of GraalVM SDK in maintainer feedback loops.
- Rationale: GraalVM Native Image is a niche, opt-in deployment target — most Spring Boot backends ship on the JVM. Adding the category would force GraalVM SDK into the maintainer build (build time 30s → 5–10 min, which destroys the fast-feedback property the methodology depends on), and external portability fixtures rarely declare native compatibility, so the rules would be vacuous (no fixture to verify against). When the team needs Native Image, the rules belong in a separate template (e.g. `ax-template-native`), not this one.
- Alternatives considered: ACCEPT category with 3 rules (rejected — build time regression unacceptable); ACCEPT as opt-in profile only (rejected — runs as vacuous in the default profile); separate template (deferred, not blocking this decision).
- Re-evaluation trigger: Spring Boot defaulting to native image in a future major release, OR ax-template being explicitly forked for serverless/Lambda deployment.

## balance-strict-25-percent — DEFERRED (advisory retained)
- Date: 2026-05-16 (re-affirmed)
- Decision: DEFER — balance stays **advisory only** (`gating.binary_only: true` in `rubric.yaml`).
- Evidence considered: none — the 25% threshold itself was a maintainer choice during ralplan, not externally anchored. Current catalog is 64 rules across 21 categories — well below 25% in any single category.
- Rationale: The 25% balance threshold is a **maintainer-set axiom**. Promoting it to a hard gate at 64 rules would lock in an opinion without external evidence; one of the categories could legitimately spike to 26% during natural growth (e.g. concentrated work on `persistence-` items) and trigger a meaningless merge block.
- Alternatives considered: ACCEPT 25% as hard gate (rejected — no data); RAISE to 40% then hard-gate (rejected — still arbitrary, same provenance gap); KEEP advisory (chosen).
- Re-evaluation trigger: empirical data showing that category-prefix concentration above some threshold N% causes measurable rule-quality regression — requires ≥ 30 rules of history *plus* a recorded incident where balance imbalance led to false confidence (e.g. security gap covered by overweight `error-` rules).

---

# Known limitations history

## ~~Snapshot bodies are navigation indexes~~ — RESOLVED 2026-05-15

- First noted: 2026-05-15
- Symptom (historical): the initial snapshots (`spring-boot-3.5`, `spring-security-6.x`)
  were fetched from `https://docs.spring.io/.../htmlsingle/index.html` landing pages —
  navigation indexes without topic prose. Keyword greps for `JOIN FETCH`, `Transactional`,
  `CGLIB`, `mass assignment` all returned MISS.
- Resolution (same day): rewrote `practices/upstream/fetch.sh` `SNAPSHOTS` array to 10
  topic-level deep references — Spring Framework reference sections, Spring Boot testing,
  Spring Data JPA query-methods, OWASP Mass Assignment Cheat Sheet, REST-assured Usage
  wiki, CWE-915. Confirmed 9/10 snapshots return ≥ 90 KB of real prose with keyword hits;
  `spring-jpa-fetching` URL corrected after WebSearch (the old `/jpa/fetching.html` path
  is a 404 — switched to `/jpa/query-methods.html`).
- All 7 existing rules now carry both `upstream_id`-shape evidence (with quoted
  substring extracted directly from the relevant snapshot) and the original
  `source_type: external` citations.
- Next refinement (still backlog): add an advisory `quote-match-check.sh` that asserts
  each `quote` field is a substring of the referenced snapshot's stripped text. Until
  then, drift between rule and source is caught only by humans during PR review.

---

# P2-D — final disposition 2026-05-16 (supersedes 2026-05-15)

The 2026-05-15 disposition kept all four candidates as DEFERRED. The 2026-05-16 user
sign-off resolves three of them to REJECT and leaves the fourth at DEFER:

| Candidate | 2026-05-15 | 2026-05-16 | Effect |
|-----------|-----------|-----------|--------|
| `quality-records-over-lombok` | DEFER | **REJECT** | Closed permanently. Records-vs-Lombok is style preference; no provenance available. |
| `arch-hexagonal-mandatory` (and other style-specific arch rules) | DEFER | **REJECT (new rules)** | The arch category is closed at the current 3 universal rules (cyclic / layer / jpa). No new style-specific arch rules are accepted. |
| `native-graalvm-required` (and the `native-` category itself) | DEFER | **REJECT (category)** | The `native-` category is removed from `rubric.yaml`. Catalog narrows **22 → 21**. |
| `balance-strict-25-percent` | DEFER | **DEFER** | Re-affirmed. Balance stays advisory. |

The catalog is now 21 advisory categories (was 22). 64 rules are distributed across 21
of those categories — every category currently shipped has ≥ 3 rules except where the
category itself is the universal-only kind (arch). No category exceeds the 25% balance
threshold; balance remains advisory.

The provenance trail behind each disposition is the corresponding entry above
(quality-records-over-lombok, arch-hexagonal-mandatory, native-graalvm-required,
balance-strict-25-percent). Re-evaluation triggers are documented per-candidate; without
the trigger event, do not relitigate.

# Audit

- Last reviewed: 2026-05-16
- Next scheduled review: when rule count crosses 100, or when a snapshot in `_MANIFEST.yaml` becomes stale (> 90 days, caught by `time_decay_guard`).
- 2026-05-15 — DECISIONS-P3.md signed: multi-layer hard enforcement activated (`.githooks/pre-commit` + `.claude/settings.local.json` PreToolUse + `practices-sentinel.yml`).
- 2026-05-16 — P2-D resolved: D1/D2/D3 REJECTED, D4 DEFERRED. Catalog narrows 22 → 21 categories (native removed). `rubric.yaml.advisory_metrics.balance.categories` updated. `MAINTAINER.md §4` updated.
