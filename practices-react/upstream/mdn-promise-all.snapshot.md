# Snapshot: MDN — Promise.all()

- **source**: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all
- **role**: primitive-semantics
- **fetched_at**: 2026-05-16T00:00:00Z
- **via**: WebFetch

## Rejection semantics (verbatim)

> "It rejects when any of the input's promises rejects, with this first rejection reason."

> "Promise.all is rejected if any of the elements are rejected. For example, if you pass in four promises that resolve after a timeout and one promise that rejects immediately, then Promise.all will reject immediately."

This is the **fail-fast** behavior. Single rejection → entire aggregate rejects.

## Aggregation semantics (verbatim)

> "This returned promise fulfills when all of the input's promises fulfill (including when an empty iterable is passed), with an array of the fulfillment values."

> "If the iterable contains non-promise values, they will be ignored, but still counted in the returned promise array value (if the promise is fulfilled)."

## Comparison to allSettled (verbatim)

> "In comparison, the promise returned by Promise.allSettled() will wait for all input promises to complete, regardless of whether or not one rejects. Use allSettled() if you need the final result of every promise in the input iterable."

## Critical clarification for catalog rules

MDN treats `Promise.all` as an **aggregation primitive** — it accepts an iterable of already-existing promises (or values). It does not start any work. Any "parallelization" effect is from when the underlying promise-returning calls are invoked, not from wrapping them in `Promise.all`.

This invalidates the Vercel seed rule's "3 round trips → 1 round trip" framing.
