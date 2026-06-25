---
title: A parent-scoped child collection that elects one preferred member MUST keep AT MOST ONE default — setting a member default atomically clears every other member's default in the same transaction (clear-all-then-set-one), the empty→first-member transition auto-defaults the sole member, and a partial unique index backstops the invariant so a torn or concurrent write can never leave two defaults
impact: MEDIUM
impactDescription: "A customer address book, a wallet of payment methods, a set of phone numbers — any collection that elects ONE preferred member — silently corrupts when two rows both carry the default flag: a checkout that reads 'the default shipping address' gets a nondeterministic one of the two, an automated charge hits an arbitrary 'default' card, and the bug is invisible until a customer is shipped to or charged on the wrong instrument. The naive implementation (set this row default) without clearing the others leaves N defaults after N set-default clicks; the concurrent implementation (clear-then-set without a parent lock or a backstop index) lets two simultaneous set-default requests both clear-then-set and end with two defaults. Clearing all other defaults in the same transaction as the set, under the parent's lock, plus a partial unique index UNIQUE(parent_id) WHERE is_default, make 'two defaults' unrepresentable; auto-defaulting the first member makes 'a non-empty collection with no default' unrepresentable."
tags:
  - data-integrity
  - collection
  - concurrency
  - default-flag
spec_ref: "specs/default-member-singleton-l0.yaml#DEFAULT-SINGLETON-001"
verification:
  type: review
  source: "specs/default-member-singleton-l0.yaml#DEFAULT-SINGLETON-001 + specs/default-member-singleton-l0.yaml#DEFAULT-SINGLETON-002"
  pattern: "A parent-scoped child collection that carries a default/primary flag enforces AT MOST ONE default per parent: setting member M default runs, in ONE transaction under the parent's @Version row lock, a clear-all (UPDATE children SET is_default=false WHERE parent_id=:p AND is_default=true) THEN sets M default (clear-all-then-set-one) — never a bare set that leaves the prior default standing; a PARTIAL UNIQUE INDEX UNIQUE(parent_id) WHERE is_default backstops it so a torn/concurrent write that would persist two defaults fails loud (409), never silently. The first child added to an empty parent is auto-defaulted, so a non-empty collection always has exactly one default. Distinct from ordered-collection's total-order position permutation and from a mandatory single child."
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-profile/src/main/java/org/broadleafcommerce/profile/core/service/CustomerAddressServiceImpl.java"
  - "https://www.postgresql.org/docs/current/indexes-partial.html"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) CustomerAddressServiceImpl.saveCustomerAddress — the empty→first-member auto-default absorbed: when the customer has no active addresses, the new address is forced default"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-profile/src/main/java/org/broadleafcommerce/profile/core/service/CustomerAddressServiceImpl.java"
    quote: "customerAddress.getAddress().setDefault(true);"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) CustomerAddressServiceImpl.saveCustomerAddress — the set-one-unsets-others trigger absorbed: when the saved address is default, makeCustomerAddressDefault clears all other defaults for the customer then sets this one"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-profile/src/main/java/org/broadleafcommerce/profile/core/service/CustomerAddressServiceImpl.java"
    quote: "customerAddressDao.makeCustomerAddressDefault(customerAddress.getId(), customer.getId());"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) CustomerAddress ORM — the clear-all-other-defaults bulk UPDATE absorbed (BC_CLEAR_DEFAULT_ADDRESS_BY_IDS): the clear-then-set discipline's clear step"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-profile/src/main/resources/config/bc/jpa/domain/CustomerAddress.orm.xml"
    quote: "SET a.isDefault = false"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "PostgreSQL documentation — Partial Indexes (Example 11.3, Setting up a Partial Unique Index): the canonical 'at most one row satisfying the predicate' backstop for the default-singleton invariant"
    url: "https://www.postgresql.org/docs/current/indexes-partial.html"
    quote: "This enforces uniqueness among the rows that satisfy the index predicate, without constraining those that do not."
    quoted_at: "2026-06-25"
---

## Rule

Many collections elect ONE **preferred** member — a customer's **default shipping address**, a wallet's **primary card**, an account's **primary phone**. The correctness obligation is a *singleton flag scoped to the parent*: at most one child per parent carries the `default` flag, and a non-empty collection has exactly one. Two facets, both from Broadleaf's customer address book:

1. **Clear-all-then-set-one (DEFAULT-SINGLETON-001).** Setting member `M` default MUST, in **one transaction** under the parent's `@Version` row lock, first **clear** the default flag on every other active child of that parent (`UPDATE children SET is_default=false WHERE parent_id=:p AND is_default=true`) and **then** set `M` default. A bare "set this row default" that does not clear the others leaves N defaults after N clicks. A clear-then-set without the parent lock lets two concurrent set-default requests interleave and both win. A **partial unique index** `UNIQUE(parent_id) WHERE is_default` is the structural backstop — per PostgreSQL, *this enforces uniqueness among the rows that satisfy the index predicate, without constraining those that do not* — so a torn or concurrent write that would persist two defaults fails loud (409), never silently.
2. **Empty→first auto-default (DEFAULT-SINGLETON-002).** The first child added to a parent with zero active children is auto-defaulted, so a non-empty collection never has *zero* default. (At-most-one from facet 1 + at-least-one-when-non-empty from facet 2 = exactly one.)

This is a **singleton-flag-within-a-collection** shape — distinct from `ordered-collection`'s total-order permutation (a unique `position` across ALL siblings) and from a parent's mandatory single child (a required 1:1, not a mutable winner-takes-all flag over a variable set).

**Correct — clear-all-then-set-one under the parent lock, partial-unique backstop, empty→auto-default:**

```java
// set member M default: clear every other default for this parent, THEN set M — one transaction, parent @Version lock
@Transactional
public void makeDefault(UUID parentId, UUID memberId) {
    parents.findByIdForUpdate(parentId);                                   // @Version row lock serializes concurrent set-default
    children.clearDefaults(parentId);   // UPDATE children SET is_default=false WHERE parent_id=:p AND is_default=true
    children.setDefault(memberId);      // then set the one
}
// add: the first member of an empty collection auto-defaults
Child c = new Child(parentId, ...);
if (children.countActiveByParent(parentId) == 0) c.setDefault(true);       // empty→first auto-default
```
```sql
-- structural backstop: at most one default per parent (partial unique index)
CREATE UNIQUE INDEX ux_children_one_default ON children (parent_id) WHERE is_default;
```

**Incorrect — bare set leaves the prior default; no lock, no backstop:**

```java
public void makeDefault(UUID parentId, UUID memberId) {
    children.setDefault(memberId);   // WRONG: prior default still set → TWO defaults; concurrent calls → two winners
}
```

A bare set accumulates defaults; without the parent lock and the partial unique index, two concurrent set-default requests both persist. Clearing all other defaults in the same transaction under the parent lock, backstopped by the partial unique index, makes "two defaults" unrepresentable; auto-defaulting the first member makes "a non-empty collection with no default" unrepresentable.

Reference: [Broadleaf CustomerAddressServiceImpl (clear-all-then-set-one + empty→auto-default)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-profile/src/main/java/org/broadleafcommerce/profile/core/service/CustomerAddressServiceImpl.java)

Reference: [PostgreSQL — Partial Indexes (partial unique index enforces uniqueness on a subset)](https://www.postgresql.org/docs/current/indexes-partial.html)
