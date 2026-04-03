- Task 2 will need a real e2e framework like Playwright, so `key-flow.test.ts` remains essentially empty until then.
- No structural problems remaining from the T1 test runner setup. It executes cleanly and passes all programmatic verification checks.
- The required npm invocation currently warns that `--list` is treated as an npm config flag; although this does not block exit-0 in the current setup, the warning indicates future npm behavior drift risk.
- Browser binaries are not provisioned in this task scope; real execution of browser steps is deferred to the later evidence-capture task after runtime prerequisites are installed.
- `npm --prefix frontend exec playwright test ... --reporter=line` still emits an npm warning because npm interprets `--reporter` as npm config before forwarding to Playwright; command still executes and now exits 0.
- Full TypeScript project typecheck remains unconfigured in this minimal frontend setup (no project tsconfig baseline), so `tsc` validation is noisy and not a reliable gate yet.

## Spring Dependency Visibility (2026-04-03)
- Spring Security and Springdoc lack exact local pins. We rely on the Spring Boot dependency management train for Security, and Springdoc is completely unpinned, which reduces evidence strength.
- npm still warns when forwarding `--reporter` through `npm exec`, but the Playwright command executes successfully; this is noise, not a current blocker.
- T5 refresh: Residual risk remains for Spring-stack freshness. All other blockers successfully removed.

- **Chub Tool Unreliability**: The `chub` tool's inability to retrieve Spring-stack data remains a recurring tooling limitation, requiring manual fallback strategies.

- **Unresolved Freshness Blockers**: The Spring-stack freshness issue still prevents curated promotion and needs targeted resolution or a formal waiver on re-entry.
