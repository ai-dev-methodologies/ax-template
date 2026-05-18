## SEALED — Recipe L4 Sub-Agent Acceptance Prompt

This prompt is **sealed** at the same commit as the recipe RECIPE.md. It MUST NOT be edited
after the sub-agent is invoked.

**Sealed at commit**: `sealed_at_commit: 348c140`
**Source of pass/fail**: `l4-sealed-rubric.md` (sibling file, also sealed)

---

## The prompt given to the sub-agent (verbatim, no edits)

```
You are a general-purpose engineering agent. You have no context from any
prior conversation. You have been given exactly two files:

  1. recipes/e-commerce/RECIPE.md
  2. practices/AGENTS.md

Using ONLY these two files, describe the L4 domain composition for the
e-commerce business pattern. Your answer must cover:

a) Which L4 domains to enable and in what dependency order (show the wiring
   sequence from least to most dependent).

b) Which L2 UI blocks to assemble, grouped by the L3 page template they
   belong to.

c) Which L3 page templates to use as scaffolding (list all of them).

d) At least 2 business invariants this composition must enforce, each with
   its spec_ref or rule_ref.

Constraints:
- Use ONLY the information in the two provided files. Do not infer from
  general knowledge about e-commerce products.
- Do not hallucinate L4 domains, L2 blocks, or L3 pages that are not
  listed in RECIPE.md.
- Quote at least one line from RECIPE.md frontmatter to confirm you read it.

Format your answer as structured markdown with clear sections for
(a), (b), (c), and (d).
```

---

## Post-execution: how the result is evaluated

See `l4-sealed-rubric.md` (sibling file). Results recorded in `l4-subagent-test.md`.
