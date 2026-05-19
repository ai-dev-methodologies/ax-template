# Codex PR #8 RE-review #2

## Verdict

APPROVE.

## Closure

The single remaining blocker is closed.

`templates/L4/webhook/README.md:42` no longer documents body-only
`HMAC-SHA256(secret, body)`. It now defines the signature as
`HMAC-SHA256(secret, signed_input)` and states that `signed_input` is the
canonical `<timestamp>.<body>` string from `WEBHOOK-SIGN-002`.

The README summary table is now consistent with `specs/webhook-l0.yaml:40-49`
for `WEBHOOK-SIGN-001`, with `specs/webhook-l0.yaml:57-67` for
`WEBHOOK-SIGN-002`, and with the README implementation snippet at
`templates/L4/webhook/README.md:63-68`.

Confirmed no stale body-only MAC wording remains:
`grep -nE "HMAC-SHA256\\(secret, body\\)" templates/L4/webhook/README.md specs/webhook-l0.yaml`
returned zero matches.

## Regression

PASS: `bash skills/_tests/L4/webhook-domain.test.sh` -> 15 passed, 0 failed.

## Final reasoning

Fix-cycle #2 was a surgical README correction, and it resolves the exact
merge blocker from the prior re-review. The public template documentation,
spec YAML, and implementation guidance now all describe the same
replay-resistant timestamp-plus-body MAC input.

No regression was observed in the focused webhook-domain guard.

## Merge recommendation

APPROVE. Merge `feat` into `main`.
