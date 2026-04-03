- Stage 3 closeout and state sync completed. State accurately reflects completion of scaffold, architecture baseline, quality companion, and manifest assets.
- Ensure state movement points to the correct immediate next work without changing curated or stable status.

- Created command matrix explicitly marking missing commands.

- Corrected command matrix: removed speculative prerequisites and aligned evidence filenames with curated/procedure/task prefixes.

- Task 4 backend skeleton should stay structure-first: explicit `auth`, `security`, `user` package boundaries and no controller business logic.
- `application.yml` baseline needs policy markers for provider flags, stateful refresh intent, RBAC roles, and CSRF/CORS requirements even before implementation.

- `/auth/me` read model must remain strictly minimal; extra skeleton-only flags drift contract fit and should not appear in the view shape.
- Frontend skeleton created with strict adherence to /auth/me-driven state to avoid drift.
- Updated React to version 19 in frontend/package.json to match local docs.
- Populated frontend directories with minimal placeholder files to establish explicit boundaries without implementing full UI.

- Task 6 verify slice should remain structure-only: manifest-driven intent documented in `verify/README.md` and `verify/scripts/README.md`, without implementing runtime checks yet.
- Fixture taxonomy is now explicitly split into `golden`, `violation`, and `false-positive` placeholder directories to align with verify triplet test planning.
- T3-4: Standardized evidence generation enforces explicit alignment checks between planned baselines and actual skeleton realization.
- T3-4 fix: Evidence files must accurately reflect the real project structure (e.g. com/ax/template/authblueprint) rather than hypothetical frameworks.
- T4-1: Test asset placeholders are stronger when scenario IDs mirror quality-companion flow names directly, especially key-flow E2E (`signup -> unverified -> verify -> login -> protected route`).
- T4-1: Verify triplet scaffolding benefits from per-axis fixture placeholders in each bucket (golden/violation/false-positive) so later execution can consume deterministic case IDs.
- T4-2: Backend tests execute successfully with Gradle, but frontend, verify, and e2e testing commands are blocked due to missing runner scripts and skeleton-only setups.
- T4-3: A minimal local verify harness unblocked the triplet by consuming manifest required keys from `verify/manifest.schema.json`, checking top-level key presence in `blueprints/auth-manifest.yaml`, and executing fixture buckets (`golden`, `violation`, `false-positive`) with explicit outcomes.
- T5-1: Chub returned fresh results for `react` (v19.2.4) but returned no results for `spring-boot`, `spring-security`, or Spring-specific `openapi` tools. This indicates we must rely on fallback docs or existing knowledge for the backend.

- The fail-open audit successfully surfaced boundary gaps (frontend, e2e) despite solid backend verify triplet results.

- The verify triplet specifically refers to golden, violation, and false-positive security testing outcomes, not standard build/lint/test steps.
- Creating a strict checklist correlation maps high-level gating criteria to actual execution evidence, eliminating guesswork.
- Evidence correlation should refer to the canonical source/manifest files, not just draft markdown copies.
- [T6] Evaluated evidence for curated promotion. Found blockers preventing promotion.

## Evidence Consistency Fix (F2 Wave)
- **Pattern**: Ensure evidence artifacts match timeline progression. If a component (e.g., verify triplet) was initially blocked in planning but became runnable during execution (T4-3), the final evidence matrix must reflect its executed (PASS) state rather than a stale BLOCKED state to prevent contradictions.
