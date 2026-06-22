---
title: "fixture — java fence references a fabricated store call + a drifted state-machine symbol with NO annotation"
---

## Fixture rule (deliberately broken)

The java fence below calls `idempotencyStore.computeIfAbsent` (a method the
shipped store does not expose) and names `FabricatedStateMachine` (no backing
`.java`), and the rule carries **no** `catalog-example-ok` annotation — so the
guard must BLOCK it.

```java
public Receipt charge(String key, ChargeRequest req) {
    return idempotencyStore.computeIfAbsent(key, k -> doCharge(req));
}

FabricatedStateMachine.transition(payment, Trigger.SUCCEEDED);
```
