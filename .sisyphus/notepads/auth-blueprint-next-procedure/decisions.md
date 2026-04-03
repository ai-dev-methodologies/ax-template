- Updated ACTIVE-LOOP.md to reflect Stage 3 canonical draft creation completion without promoting to Stage 4 curated status.
- Did not change the plan target in .sisyphus/boulder.json since it correctly points to auth-blueprint-next-procedure, though session metadata changes during orchestration.
- Removed the incorrectly generated file paths from the `ACTIVE-LOOP.md` evidence list, preserving only the valid ones that actually exist.

- Decided to use a dedicated draft doc (.sisyphus/drafts/auth-blueprint-execution-command-matrix.md) to freeze the matrix.

- Corrected command matrix to use factual local status (no package.json, etc.) instead of guessing.

- For execution Task 4 slice, limited output to backend scaffold only: Gradle baseline, Java package skeleton, resources baseline, and test tree placeholders.
- Chose Spring Security resource server configuration as the default skeleton path and avoided custom JWT filter scaffolding per architecture baseline.

- Removed explicit stateless session policy from security skeleton to avoid contradicting the stateful refresh baseline while keeping resource-server direction unchanged.
- Implemented refresh queue/mutex inside the auth boundary as per architecture baseline.
- Aligned auth state model with specific /auth/me contract (userId, email, roles, providerLinks, verificationState).
- Adopted lightweight placeholder patterns for testing and features to prevent empty directory issues in verification.

- Preserved existing `verify/manifest.schema.json` placeholder unchanged and built only minimal adjacent scaffold required by Task 6.
- Chose README-first verify skeleton documentation (top-level + scripts + fixtures map) to keep fail-open prohibition and manifest-driven routing expectations audit-visible before code implementation.
- T3-4: Documented skeleton drift checks exactly as constrained by baseline (no custom JWT filters, minimal /auth/me, fail-closed).
- T3-4 fix: Updated scaffold realization evidence with exact, verified local paths instead of fabricated paths.
- T4-1: Kept implementation strictly at asset layer by expanding placeholder tests and fixture files only, with no runtime auth code changes.
- T4-1: Bound key-flow E2E placeholder scope to the quality companion’s exact three flows (main signup/verify/login path, provider login path, provider disabled fallback path).
- T4-2: Recorded blocked axes honestly in the test-matrix evidence rather than skipping them, ensuring downstream awareness of what is and isn't testable yet.
- T4-3: Implemented `python3 verify/scripts/run_verify_triplet.py` as the smallest runnable command and kept semantics fixture-driven (`expected` + bucket/case IDs) to avoid inventing policy logic beyond manifest/schema + placeholder categories.
- Recorded honest evidence for chub freshness checks in `.sisyphus/evidence/curated-chub-results.md`, avoiding fabrication of missing docs.
- Explicitly marked `spring-boot`, `spring-security`, and `openapi` (for Java) as unavailable in chub, setting expectations for downstream tasks.

- Documented missing frontend test script, missing e2e infrastructure, and unavailable chub data as explicit blockers rather than faking closure.

- Corrected fail-open audit wording to accurately reflect verify triplet terminology (golden, violation, false-positive).
- Explicitly marked curated gate as BLOCKED due to `chub` freshness for Spring-stack and missing frontend/e2e tests, mapping directly from existing evidence.
- Replaced the indirect reference to the markdown manifest draft with the real canonical local path `blueprints/auth-manifest.yaml` in the checklist correlation evidence.
- [T6] Decision: draft 유지. Curated promotion rejected due to missing frontend tests, e2e infrastructure, and unavailable Spring-stack chub.

## Evidence Consistency Fix (F2 Wave)
- **Decision**: Update `procedure-4-test-matrix.txt` to show the verify triplet as PASS instead of BLOCKED.
- **Rationale**: To resolve contradiction with `procedure-4-verify-triplet.txt` which legitimately shows a successful execution run made possible during T4-3. This maintains historical honesty without fabricating false success for genuinely blocked items (frontend, e2e).
