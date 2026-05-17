---
name: ax-scaffold
description: >
  Tier-1 domain scaffolding skill. Generates an L4 domain skeleton (Spec Trio +
  templates/L4/<domain>/ stubs) per Appendix C of METHODOLOGY.md. Use when adding
  a new domain to the composition kit. Accepts a required <domain> argument.
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
  importPatterns: []
retrieval:
  aliases:
    - ax-scaffold
    - scaffold domain
    - new domain
    - add domain
    - bootstrap domain
  intents:
    - create a new backend/frontend domain
    - scaffold Spec Trio for a new domain
    - generate L4 template stubs
    - follow Appendix C recipe
  entities:
    - new-domain.sh
    - Spec Trio
    - L4
    - METHODOLOGY.md
    - trio_integrity_allowlist.yaml
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
specs/<domain>-frontend-l0.yaml
specs/<domain>-asvs-l0.yaml          (backend, if full_trio)
contracts/<domain>-openapi.yaml       (backend)
contracts/<domain>-ui.yaml            (frontend)
blueprints/<domain>-manifest.yaml     (backend)
blueprints/<domain>-ui-manifest.yaml  (frontend)
templates/L4/<domain>/                (frontend workload stubs)
```

### Step 4: Allowlist registration
After scaffold, manually add the domain to
`practices/evals/trio_integrity_allowlist.yaml` under the correct mode.
The `trio_integrity_guard.sh` will FAIL with `ZERO_SCAN` if the domain is absent.

### Step 5: TDD anchor
Write a failing test before any implementation. The test name must contain the
domain name so it is selectable via `--tests *<Domain>*`.

## Bundled scripts
- `skills/ax-scaffold/scripts/new-domain.sh` — generates domain skeleton per METHODOLOGY.md Appendix C; accepts `<domain>` + `--dry-run`; exit 0 on success
- `skills/ax-scaffold/scripts/validate-domain-name.sh` — checks domain name is lowercase-kebab, no special chars; called by new-domain.sh

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
bash skills/ax-scaffold/scripts/new-domain.sh dummy-domain --dry-run
# Expected: exit 0, prints file plan, no files written
```
