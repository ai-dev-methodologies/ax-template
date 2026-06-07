---
name: ax-scaffold
description: >
  Tier-1 domain scaffolding skill. Generates an L4 domain skeleton (Spec Trio +
  templates/L4/<domain>/ stubs) per Appendix C of METHODOLOGY.md, OR wires a
  business composition recipe across multiple existing L4 domains via the `business`
  subcommand. Use `ax-scaffold <domain>` for new domains; use `ax-scaffold business
  <pattern> <project-name>` for business pattern composition.
metadata:
  priority: 1
  tier: 1
  axis: root
  docs:
    - "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/"
    - "https://nextjs.org/docs/app/building-your-application"
  pathPatterns:
    - 'skills/ax-scaffold/SKILL.md'
    - 'METHODOLOGY.md'
  bashPatterns:
    - 'bash skills/ax-scaffold/scripts/new-domain.sh'
    - 'bash skills/ax-scaffold/scripts/new-business-recipe.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-scaffold
    - scaffold domain
    - new domain
    - add domain
    - bootstrap domain
    - scaffold business
    - business recipe
    - wire recipe
  intents:
    - create a new backend/frontend domain
    - scaffold Spec Trio for a new domain
    - generate L4 template stubs
    - follow Appendix C recipe
    - wire a business pattern recipe across multiple L4 domains
    - scaffold saas-subscription / e-commerce / crm composition
  entities:
    - new-domain.sh
    - new-business-recipe.sh
    - Spec Trio
    - L4
    - METHODOLOGY.md
    - trio_integrity_allowlist.yaml
    - recipes/
---

# ax-scaffold

Tier-1 domain scaffolding skill. Generates the skeleton for a new domain following
METHODOLOGY.md Appendix C Recipe B (the 5-step playbook). This skill is the only
sanctioned way to add a new L4 domain — it ensures Spec Trio files, domain mode
classification, and TDD anchor are in place before any implementation starts.

Invoke with: `/ax-scaffold <domain-name> [--dry-run]`

`--dry-run` prints the file plan without writing anything. Use this before
committing to a new domain to confirm the scaffold matches expectations.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Determine domain mode (`full_trio` / `backend_only` / `frontend_only`)
- [ ] Step 2: Run scaffold — `bash skills/ax-scaffold/scripts/new-domain.sh <domain> [--dry-run]`
- [ ] Step 3: Inspect generated Spec Trio files — confirm schema fields are present
- [ ] Step 4: Add domain to `practices/evals/trio_integrity_allowlist.yaml`
- [ ] Step 5: Write TDD anchor test (RED) before touching implementation
- [ ] Step 6: Run `/ax-verify-domain <domain>` to confirm scaffold is structurally valid
- [ ] Step 7: Commit scaffold files; do NOT mix scaffold + implementation commits

## Steps detail

### Step 1: Determine domain mode
Read `practices/evals/trio_integrity_allowlist.yaml`. Choose:
- `full_trio`: domain has both backend API and frontend UI
- `backend_only`: domain has backend API only (no UI)
- `frontend_only`: domain has frontend UI only (static viewer; no backend OpenAPI)

### Step 2: Run scaffold
Script: `skills/ax-scaffold/scripts/new-domain.sh`
Args: `<domain>` (required), `--dry-run` (optional).
Exit semantics: 0 = files written (or plan printed); non-zero = validation error
(e.g., domain already exists, invalid domain name, missing METHODOLOGY.md).

Generated file tree (for `full_trio`):
```
specs/<domain>-l0.yaml                 (backend compliance — canonical name)
specs/<domain>-frontend-l0.yaml        (frontend page-compliance)
contracts/<domain>-openapi.yaml        (backend)
contracts/<domain>-ui.yaml             (frontend)
blueprints/<domain>-manifest.yaml      (backend)
blueprints/<domain>-ui-manifest.yaml   (frontend)
templates/L4/<domain>/                 (frontend workload stubs)
```

Every generated spec is an **UNFILLED skeleton** carrying a `# TODO: Add` marker.
`spec_scaffold_unfilled_guard.sh` (hard gate [70]) BLOCKS the build until it is filled,
so the **required next step is `/ax-plan <domain>`** (the G6 forcing wire): /ax-plan
runs the interview, fills the Spec Trio with real items, and emits 1:1 RED `@Tag` stubs.
Do not hand-write items or implementation before running /ax-plan.

### Step 4: Allowlist registration
After scaffold, manually add the domain to
`practices/evals/trio_integrity_allowlist.yaml` under the correct mode.
The `trio_integrity_guard.sh` will FAIL with `ZERO_SCAN` if the domain is absent.

### Step 5: TDD anchor
Write a failing test before any implementation. The test name must contain the
domain name so it is selectable via `--tests *<Domain>*`.

## Business subcommand

Invoke with: `/ax-scaffold business <pattern> <project-name> [--dry-run]`

Wires a business pattern recipe that spans multiple existing L4 domains. The recipe
is defined in `recipes/<pattern>/RECIPE.md` (YAML frontmatter) and its machine-readable
spec is at `specs/recipes/<pattern>-recipe-l0.yaml`.

Available patterns (see `recipes/` directory): `saas-subscription`, `e-commerce`, `crm`.

`--dry-run` prints the file plan — enabled L4 domains (with catalog existence check)
and L2 blocks to wire — without writing anything or running verification.

In normal mode, the subcommand:
1. Verifies each enabled L4 domain exists in `templates/L4/`
2. Runs `/ax-verify-domain <domain>` for each enabled L4 (inline loop; no new Tier-2 skill)
3. Writes `<project-name>/business-composition.yaml` as the wiring manifest

## Business recipe checklist (copyable)

- [ ] Step 1: Choose a pattern — `saas-subscription`, `e-commerce`, or `crm`
- [ ] Step 2: Dry-run — `bash skills/ax-scaffold/scripts/new-business-recipe.sh <pattern> <project> --dry-run`
- [ ] Step 3: Confirm the enabled L4 domain list matches your business scope (use inline `override_allowed:` in RECIPE.md if adjusting)
- [ ] Step 4: Apply — run without `--dry-run` to write `business-composition.yaml` and verify all L4 domains
- [ ] Step 5: Annotate each enabled L4 domain README with `applied_recipe: <pattern>`
- [ ] Step 6: Implement business logic using the L4 catalog + L2 blocks listed in the recipe

## Bundled scripts
- `skills/ax-scaffold/scripts/new-domain.sh` — generates domain skeleton per METHODOLOGY.md Appendix C; accepts `<domain>` + `--dry-run`; exit 0 on success
- `skills/ax-scaffold/scripts/validate-domain-name.sh` — checks domain name is lowercase-kebab, no special chars; called by new-domain.sh
- `skills/ax-scaffold/scripts/new-business-recipe.sh` — wires a business pattern recipe; reads `recipes/<pattern>/RECIPE.md`; accepts `<pattern> <project-name>` + `--dry-run`; exit 0 on success

## Feedback loop
When scaffold fails: stderr contains the validation message.
Common errors: `DOMAIN_EXISTS` (already scaffolded), `INVALID_NAME` (bad chars),
`MISSING_METHODOLOGY` (METHODOLOGY.md not found at repo root).
For each: fix the argument or repo state, then re-run.
Halt threshold: if scaffold produces files but `/ax-verify-domain <domain>` fails
after 3 fix attempts, open an issue in DECISIONS.md and halt.

## Invocation graph
- Calls (Tier-2): `/ax-verify-domain <domain>` for post-scaffold validation
- Called by (Tier-1): user directly

## Acceptance (binary)
```bash
# Domain scaffold
bash skills/ax-scaffold/scripts/new-domain.sh dummy-domain --dry-run
# Expected: exit 0, prints file plan, no files written

# Business recipe scaffold
bash skills/ax-scaffold/scripts/new-business-recipe.sh saas-subscription test-saas --dry-run
# Expected: exit 0, prints enabled L4 list + L2 blocks, no files written
```
