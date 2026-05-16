# Snapshot: MDN — Promise.allSettled()

- **source**: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled
- **role**: primitive-semantics
- **fetched_at**: 2026-05-16T00:00:00Z
- **via**: WebFetch

## When to use (verbatim)

> "Promise.allSettled() is typically used when you have multiple asynchronous tasks that are not dependent on one another to complete successfully, or you'd always like to know the result of each promise."

> "In comparison, the Promise returned by Promise.all() may be more appropriate if the tasks are dependent on each other, or if you'd like to immediately reject upon any of them rejecting."

## Result shape

Each outcome object has:

- `status`: `"fulfilled"` or `"rejected"`
- `value` (only if `status` === `"fulfilled"`)
- `reason` (only if `status` === `"rejected"`)

## Example (verbatim)

```javascript
Promise.allSettled([
  Promise.resolve(33),
  new Promise((resolve) => setTimeout(() => resolve(66), 0)),
  99,
  Promise.reject(new Error("an error")),
]).then((values) => console.log(values));

// [
//   { status: 'fulfilled', value: 33 },
//   { status: 'fulfilled', value: 66 },
//   { status: 'fulfilled', value: 99 },
//   { status: 'rejected', reason: Error: an error }
// ]
```

## Settle semantics

- Empty iterable → already fulfilled
- Non-empty with no pending promises → still asynchronously (not synchronously) fulfilled
- Waits for ALL inputs to settle regardless of any individual rejection

## Audit implication

Any rule about Promise.all must cross-reference allSettled for partial-failure handling. Vercel's seed rule omitted this; Next.js docs explicitly call it out.
