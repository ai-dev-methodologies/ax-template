---
title: "fixture — same divergent symbols, but the divergence is declared inline so the guard passes"
---

## Fixture rule (annotated, intentionally generic)

Identical java fence to the fail fixture, but every divergent symbol is named
in a `catalog-example-ok` annotation — the single auditable escape hatch — so
the guard treats the illustration as a deliberate generic example.

<!-- catalog-example-ok: IdempotencyKeyStore FabricatedStateMachine — intentionally generic fixture; the reference impls are IdempotencyKeyStore + PaymentStateMachine -->

```java
public Receipt charge(String key, ChargeRequest req) {
    return idempotencyStore.computeIfAbsent(key, k -> doCharge(req));
}

FabricatedStateMachine.transition(payment, Trigger.SUCCEEDED);
```
