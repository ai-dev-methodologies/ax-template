# Broadleaf-absorption parity registry

Every Broadleaf-absorbed vertical MUST have a parity record in this directory
(`docs/broadleaf-parity/<vertical>.md`). The record proves the absorption methodology
ran in full AND captured the same **verification goal** as Broadleaf's own tests — the
test INTENT (scenarios), never the test code (FUL-licensed). Enforced mechanically by
`practices/evals/broadleaf_absorption_parity_guard.sh` (run inside R25): each record's
required fields, ≥1 verification-goal parity row, and referenced artifacts (spec items,
rule, behavioral test) are validated to exist. License safety (no ported Broadleaf source
in our implementation tree) is enforced by `practices/evals/broadleaf_no_port_guard.sh`.

| # | vertical | parity record | absorbed invariant |
|---|----------|---------------|--------------------|
| G001 | promotion | [promotion.md](promotion.md) | offer engine: conserving proration · deterministic order · atomic max-uses · clamp |
| G002 | pricing | [pricing.md](pricing.md) | pricing pipeline: discount-before-tax (net base) · conserving total closure |
| G003 | order | [order.md](order.md) | cart→order spine: price snapshot · immutable-after-submit · qty merge · fulfillment conserves |
| G004 | checkout | [checkout.md](checkout.md) | saga: register-compensation-before-irreversible-side-effect (SAGA-COMPENSATE-002) |
| G005 | payment | [payment.md](payment.md) | split-tender coverage (Σ tenders ≥ order total) |
| G006 | inventory-customer | [inventory-customer.md](inventory-customer.md) | inventory tri-state gate · exactly-one-default singleton |

> Note on verification-goal parity: for the backfilled G001–G006 records the parity rows
> are the invariant's required verification scenarios, cross-checked against Broadleaf's
> source behavior. From G007 onward the parity step additionally reads Broadleaf's own
> test files to extract their test INTENT (scenarios) and maps each to one of our
> behavioral assertions — capturing the same verification goal without copying test code.
