# Codex PR #8 RE-review

## Verdict

REQUEST CHANGES.

The fix-cycle closes the Playwright INV-005 failure and the `DECISIONS.md` EOF whitespace blocker. The HMAC blocker is only partially closed: `specs/webhook-l0.yaml` is now internally consistent, but the public webhook L4 README summary still documents a body-only MAC.

## Blocker closure (3)

1. HMAC unify: PARTIAL. `specs/webhook-l0.yaml:40-49` now defines `X-Webhook-Signature` as `HMAC-SHA256(secret, signed_input)`, delegates `signed_input` to `WEBHOOK-SIGN-002`, and explicitly rejects body-only MAC. `specs/webhook-l0.yaml:59-67` matches `timestamp + "." + body`.
2. `internal-it-webhook-secret.spec.ts`: CLOSED. `specs/recipes/internal-it-recipe-l0.yaml:68-80` keeps `envelope encryption` and `encrypted ciphertext` inside INV-005 notes. Exact command from `frontend/` passed: `npx playwright test tests/recipes/internal-it-webhook-secret.spec.ts` -> 10/10.
3. `DECISIONS.md` trailing blank: CLOSED. `git diff --check main...HEAD` exits 0. File is Unix-newline terminated without a double trailing newline.

## Regression check

- PASS: `bash practices/evals/run-all-guards.sh` -> 9 passed, 0 failed.
- PASS: `bash skills/_tests/L4/scheduler-domain.test.sh` -> 12 passed, 0 failed.
- PASS: `bash skills/_tests/L4/webhook-domain.test.sh` -> 15 passed, 0 failed.
- PASS: `bash practices/evals/recipe_spec_referential_integrity_guard.sh` -> 10/10 recipe specs.
- PASS: `PYTHONPATH=/private/tmp/ax-no-yaml bash practices/evals/recipe_governance_guard.sh` -> all checks PASS.
- PASS: sealed-verdict diff only adds `internal-it-verdict.md` and `webhook-l4-verdict.md`; no existing sealed verdict files changed.

## Independent attack

BLOCKING: stale body-only MAC remains in the webhook L4 README.

`templates/L4/webhook/README.md:42` still says:

```md
Outbound `X-Webhook-Signature: sha256=<hex(HMAC-SHA256(secret, body))>`
```

That contradicts the fixed canonical contract in `specs/webhook-l0.yaml:40-49` and the README implementation snippet at `templates/L4/webhook/README.md:63-68`, which signs `timestamp + "." + body`. A fork implementer following the summary table can still produce the replay-vulnerable body-only MAC the fix-cycle intended to reject.

## Final reasoning

The local tests and guards are green, but the HMAC consistency blocker is not fully closed across the shipped webhook template documentation. This is a one-line surgical fix, but it is merge-blocking because it preserves the same contract ambiguity in a user-facing implementation guide.

## Merge recommendation

REQUEST CHANGES. Update `templates/L4/webhook/README.md:42` to use the canonical `signed_input` / `<timestamp>.<body>` MAC wording, then rerun `git diff --check main...HEAD` and `bash skills/_tests/L4/webhook-domain.test.sh`.
