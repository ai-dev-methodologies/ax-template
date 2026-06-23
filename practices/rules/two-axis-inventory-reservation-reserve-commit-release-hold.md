---
title: A two-axis available/reserved inventory must derive AVAILABLE = onHand − reserved (never store it), reserve a HELD hold only when derived available ≥ q (422 else, reserved += q, onHand untouched), COMMIT a held reservation by decrementing BOTH onHand and reserved (the goods leave), RELEASE it by decrementing reserved alone (the hold frees), move HELD → (COMMITTED|RELEASED) EXACTLY once (409 otherwise), keep reserved == Σ(HELD quantities) with 0 ≤ reserved ≤ onHand, and serialize concurrent reserves on the item row so exactly available/q win
impact: HIGH
impactDescription: "A two-axis inventory that stores 'available' as a third column lets it drift from on-hand and reserved (an order promises stock that is not there, or holds stock that was already shipped); a reserve that does not take the item row lock lets two threads both pass the available ≥ q gate against the same headroom and over-reserve below zero available (CWE-362 — oversell); a commit that forgets to decrement reserved (or a release that touches on-hand) breaks the reserved == Σ(HELD) conservation so the available projection is permanently wrong; and a non-exactly-once commit/release double-counts goods leaving or double-frees a hold"
tags:
  - state-machine
  - concurrency
  - inventory
  - conservation
  - governance
spec_ref: "specs/two-axis-inventory-reservation-l0.yaml#INVRES-RESERVE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/inventoryreservation/InventoryReservationService.java + backend/src/main/java/com/ax/template/authblueprint/inventoryreservation/InventoryItem.java + backend/src/main/java/com/ax/template/authblueprint/inventoryreservation/InventoryReservation.java + backend/src/main/java/com/ax/template/authblueprint/inventoryreservation/ReservationStateMachine.java"
  pattern: "AVAILABLE is derived (onHand − reserved) and never persisted; reserving takes the item's PESSIMISTIC_WRITE row lock, refuses with 422 INVENTORY_INSUFFICIENT_AVAILABLE when available < q, else increments reserved by q and appends an immutable Reservation row (status HELD via ReservationStateMachine) leaving onHand untouched; committing a HELD reservation decrements BOTH onHand and reserved by q and moves HELD → COMMITTED; releasing a HELD reservation decrements reserved by q (onHand untouched) and moves HELD → RELEASED; a commit/release on a non-HELD reservation is 409 INVENTORY_RESERVATION_NOT_HELD (exactly-once); a @Check reserved >= 0 AND reserved <= on_hand backstops the conservation reserved == Σ(HELD quantities); NO delete path exists"
upstream:
  - "https://docs.commercetools.com/api/projects/inventory"
  - "https://learn.microsoft.com/en-us/azure/architecture/patterns/saga"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "commercetools Composable Commerce HTTP API, InventoryEntry representation (official API reference) — the available/reserved derivation the two-axis model generalizes: available is on-stock minus reserved, not a separately stored third axis"
    url: "https://docs.commercetools.com/api/projects/inventory"
    quote: "Available amount of stock (quantityOnStock - reserved)."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "Microsoft Azure Architecture Center, Saga design pattern — the reserve-then-confirm two-phase hold: a compensable transaction reserves, a pivot transaction commits (the point of no return), and a compensating transaction releases on failure"
    url: "https://learn.microsoft.com/en-us/azure/architecture/patterns/saga"
    quote: "If a step in the saga fails, compensating transactions undo the changes that the compensable transactions made."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent reserves racing one item's available headroom)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A two-axis inventory is a derived available over an on-hand/reserved pair with a two-phase hold — not three drift-prone counters

**Impact: HIGH — a stored 'available' drifts from its axes (oversell or phantom holds); an unlocked reserve over-reserves under concurrency (CWE-362); a commit that skips reserved, or a release that touches on-hand, breaks conservation; a non-exactly-once commit/release double-ships or double-frees.**

A two-axis inventory tracks exactly two persisted quantities per item — `onHand` (the physical goods present) and `reserved` (the quantity currently held against open demand). The third quantity every order system needs, *available*, is a **pure function of the two**: as the commercetools inventory API states it, the available amount of stock is *"(quantityOnStock - reserved)"*. Storing `available` as a third column is the canonical defect: it drifts the instant a reserve updates `reserved` but not the cached `available`, and the system promises stock that is not there. The catalog conserved a transformation (`transformations`) and walked a monotone register (`registers`) but had no primitive for the two-axis hold:

```text
available(item):         DERIVED = onHand − reserved        (never a stored column)
reserve(item, q):        take the item's PESSIMISTIC_WRITE lock; if available < q → 422;
                         else reserved += q, append a HELD Reservation(q); onHand UNCHANGED  (the hold)
commit(reservation):     HELD → COMMITTED; onHand −= q AND reserved −= q                     (the goods leave)
release(reservation):    HELD → RELEASED;  reserved −= q; onHand UNCHANGED                   (the hold frees)
exactly-once:            commit/release on a non-HELD reservation → 409                       (HELD→one terminal)
conservation:            reserved == Σ(HELD quantities)  AND  0 ≤ reserved ≤ onHand           (@Check backstop)
```

**1. AVAILABLE is derived, never stored (INVRES-RESERVE-001 / INVRES-CONSERVE-001).** The entity has `on_hand` and `reserved` columns and NO `available` column; available is computed on read. This makes drift unrepresentable — the projection always reflects the two axes.

**2. The hold is a two-phase reserve → commit-or-release (INVRES-COMMIT/RELEASE-001).** Reserving only increments `reserved` (the goods stay on hand but stop being available); committing is the *pivot* that lets the goods leave (decrement BOTH axes); releasing is the *compensating* transaction that frees the hold (decrement `reserved` only). A reservation moves `HELD → (COMMITTED | RELEASED)` exactly once — the `ReservationStateMachine` rejects any second transition with a 409.

**3. Concurrent reserves serialize on the item row (INVRES-CONCURRENT-001).** The reserve path takes the item's `PESSIMISTIC_WRITE` lock so the read-available / write-reserved sequence cannot interleave; N concurrent reserves against `available = k·q` resolve to exactly k winners and N−k `422`s (CWE-362).

**Incorrect — a stored available, an unlocked over-reserving reserve, a commit that forgets reserved:**

```java
public void reserve(UUID itemId, long q) {
    InventoryItem item = repo.findById(itemId).orElseThrow();  // ❌ no row lock — two threads both read available
    if (item.getAvailable() < q) throw new IllegalStateException();  // ❌ 'available' is a STORED column that drifts
    item.setAvailable(item.getAvailable() - q);                // ❌ public setter; decremented a stored axis
    item.setReserved(item.getReserved() + q);                  // ❌ both threads pass the gate → over-reserve (CWE-362)
    repo.save(item);
}
public void commit(UUID reservationId) {
    Reservation r = reservationRepo.findById(reservationId).orElseThrow();
    r.setStatus(COMMITTED);                                    // ❌ no exactly-once guard; double-commit double-ships
    item.setOnHand(item.getOnHand() - r.getQty());             // ❌ forgot reserved -= q → conservation broken forever
}
```

**Correct — derived available, locked reserve gated on available ≥ q, two-phase commit/release decrementing the right axes:**

```java
@Transactional
public InventoryReservation reserve(UUID itemId, long quantity, String actor) {
    InventoryItem item = items.findByIdForUpdate(itemId).orElseThrow(InventoryReservationException::itemNotFound); // ✅ PESSIMISTIC_WRITE
    if (item.available() < quantity) {                          // ✅ available() is DERIVED: onHand − reserved
        throw InventoryReservationException.insufficientAvailable();   // 422 — nothing mutated
    }
    item.reserve(quantity);                                     // ✅ reserved += q; onHand untouched (the hold)
    InventoryReservation r = new InventoryReservation(UUID.randomUUID(), item.getId(), quantity,
        ReservationStatus.HELD, actor, Instant.now(clock));     // ✅ immutable HELD row appended
    return members.persistAndFlush(r);
}

@Transactional
public InventoryReservation commit(UUID reservationId) {
    InventoryReservation r = items.findReservation(reservationId).orElseThrow(InventoryReservationException::reservationNotFound);
    InventoryItem item = items.findByIdForUpdate(r.getItemId()).orElseThrow(InventoryReservationException::itemNotFound);
    stateMachine.commit(r);                                     // ✅ HELD → COMMITTED, exactly-once (409 if not HELD)
    item.commitReservation(r.getQuantity());                   // ✅ onHand −= q AND reserved −= q (goods leave)
    return r;
}

@Transactional
public InventoryReservation release(UUID reservationId) {
    InventoryReservation r = items.findReservation(reservationId).orElseThrow(InventoryReservationException::reservationNotFound);
    InventoryItem item = items.findByIdForUpdate(r.getItemId()).orElseThrow(InventoryReservationException::itemNotFound);
    stateMachine.release(r);                                    // ✅ HELD → RELEASED, exactly-once (409 if not HELD)
    item.releaseReservation(r.getQuantity());                  // ✅ reserved −= q only (the hold frees, onHand untouched)
    return r;
}
```

The item-row `PESSIMISTIC_WRITE` lock serializes the read-available / write-reserved sequence so two threads cannot both pass the `available ≥ q` gate against the same headroom (CWE-362); the `@Check reserved >= 0 AND reserved <= on_hand` is the DB backstop that makes an over-reserve or a conservation break unrepresentable. `available()` is a derived method (`onHand − reserved`), never a stored column, so it cannot drift. `InventoryReservation` rows are `@AggregateMember` of `InventoryItem` — root-JPQL reads, `common/MemberWriter` writes; the `ReservationStateMachine` is the sole status mutator; no delete path exists on either entity.

Verification: review-tier — confirm `available` is computed and never persisted, the reserve gate is `available ≥ q` under the item's `PESSIMISTIC_WRITE` lock, commit decrements BOTH on-hand and reserved while release decrements reserved alone, the `ReservationStateMachine` makes `HELD → (COMMITTED|RELEASED)` exactly-once (409 otherwise), and the `@Check reserved >= 0 AND reserved <= on_hand` backstops conservation. The behavioural proof a fork-receiver keeps green: the N-thread reserve race against `available = k·q` (exactly k 2xx + N−k 422, reserved = k·q ≤ onHand).

Reference: [commercetools Inventory API — availableQuantity = quantityOnStock − reserved](https://docs.commercetools.com/api/projects/inventory)

Reference: [Microsoft Azure Architecture Center — Saga design pattern (compensating transactions)](https://learn.microsoft.com/en-us/azure/architecture/patterns/saga)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
