## SEALED — Recipe L4 Sub-Agent Acceptance Prompt

This prompt is **sealed** at the same commit as the recipe RECIPE.md. It MUST NOT be edited
after the sub-agent is invoked. If the RECIPE.md manifest doesn't guide the sub-agent to satisfy
this prompt's constraints from a context-0 cold start, the recipe has a discoverability gap.

**Sealed at commit**: `sealed_at_commit: 348c140`
**Source of pass/fail**: `l4-sealed-rubric.md` (sibling file, also sealed)

---

## The prompt given to the sub-agent (verbatim, no edits)

```
You are a general-purpose engineering agent. You have no context from any
prior conversation. You have been given exactly two files:

  1. recipes/saas-subscription/RECIPE.md
  2. practices/AGENTS.md

Using ONLY these two files, describe the L4 domain composition for the
saas-subscription business pattern. Your answer must cover:

a) Which L4 domains to enable and in what dependency order (show the wiring
   sequence from least to most dependent).

b) Which L2 UI blocks to assemble, grouped by the L3 page template they
   belong to.

c) Which L3 page templates to use as scaffolding (list all of them).

d) At least 2 business invariants this composition must enforce, each with
   its spec_ref or rule_ref.

Constraints:
- Use ONLY the information in the two provided files. Do not infer from
  general knowledge about SaaS products.
- Do not hallucinate L4 domains, L2 blocks, or L3 pages that are not
  listed in RECIPE.md.
- Quote at least one line from RECIPE.md frontmatter to confirm you read it.

Format your answer as structured markdown with clear sections for
(a), (b), (c), and (d).
```

---

## Why this specific acceptance task?

The recipe composition discovery test was chosen because:

1. **Self-containment test** — RECIPE.md must be sufficient for a context-0 agent to reproduce
   the entire L4 wiring without any additional codebase context.
2. **No hallucination trap** — A well-formed RECIPE.md has an explicit `enabled_l4_domains:`
   list. Any agent that names domains outside this list is hallucinating from general knowledge.
3. **Invariant traceability** — The `business_invariants:` block tests whether references are
   legible and cited correctly without needing to open the referenced files.
4. **Minimal context** — The two-file constraint (RECIPE.md + AGENTS.md) mirrors the fork-receiver
   use case: a new engineer or agent opening the recipe for the first time.

---

## Post-execution: how the result is evaluated

See `l4-sealed-rubric.md` (sibling file). The rubric evaluates the sub-agent's output against
12 MUST_PASS and 8 SHOULD_PASS criteria. Results are recorded in `l4-subagent-test.md`.
