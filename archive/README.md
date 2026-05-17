# archive/

Frozen reference material. Not under active development. Kept in-repo because
it remains useful as the worked example for ax-template's methodology.

| Subdir | What | Status |
|--------|------|--------|
| `backend-reference/` | Spring Boot reference workload (auth + CRUD + rate-limit + ASVS L1 + RestAssured tests) — the original "skill applied to itself" demonstration | FROZEN v1.0 (2026-05-17) |

## Why archived (not deleted)

The Spring Boot workload was ax-template's original product face. Round 3
strategic review (2026-05-17) reframed the product around React /
`eslint-plugin-ax`. The Java side stops being active growth surface but
remains valuable as:

- proof that Spec Trio + `./gradlew test{Domain}` binary verification works
  end-to-end on a non-trivial Java codebase,
- worked example of ASVS L1 26-item compliance via RestAssured,
- reference for the practices/ rule catalog (which is also frozen — see
  `../practices/STATUS.md`).

If you are looking for the active code, see `practices-react/eslint-plugin-ax/`
at the repo root.
