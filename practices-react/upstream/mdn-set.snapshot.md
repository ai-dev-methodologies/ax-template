# Snapshot: MDN — Set

- **source**: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set
- **role**: primitive-semantics
- **fetched_at**: 2026-05-16T00:00:00Z
- **via**: WebFetch

## Performance spec (verbatim)

> "access times that are sublinear on the number of elements in the collection"

This is the TC39 guarantee. Implementations may use hash tables (O(1) amortized), search trees (O(log N)), or other structures — as long as complexity is better than O(N).

## Set.has vs Array.includes (verbatim)

> "The has method checks if a value is in the set, using an approach that is, on average, quicker than testing most of the elements that have previously been added to the set. In particular, it is, on average, faster than the Array.prototype.includes method when an array has a length equal to a set's size."

## Equality semantics

- **Primitives**: SameValueZero. `NaN === NaN` for Set purposes (special case).
- **Objects**: reference identity.

```javascript
const o = { a: 1, b: 2 };
mySet.add(o);
mySet.add({ a: 1, b: 2 }); // Different object → added separately
mySet.has(o); // true
mySet.has({ a: 1, b: 2 }); // false — different reference
```

## Audit implication

Catalog rules that say "Set lookups are O(1)" are using engineering shorthand. The formal guarantee is "sublinear". For object keys, value-equality lookups silently fail — primitives or stable references required.
