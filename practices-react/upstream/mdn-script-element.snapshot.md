# Snapshot: MDN — `<script>` element

- **source**: https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script
- **role**: primitive-semantics
- **fetched_at**: 2026-05-16T00:00:00Z
- **via**: WebFetch synthesis

## Attribute behavior (verbatim/paraphrase)

- **default** (no async/defer): blocks HTML parsing while the script downloads AND executes; executes in source order.
- **defer**: downloads in parallel; executes after document parsing completes; **preserves order** among defer scripts.
- **async**: downloads in parallel; executes as soon as ready; **no order guarantee**.

## type="module" (verbatim from MDN)

> "Module scripts are deferred by default. The defer attribute has no effect since they are deferred by default."

## Cross-origin

`script` supports `crossorigin` attribute for CORS handling — required for some preload + execute paths.

## Audit implication

Catalog rule for defer/async should note:
- `type="module"` is implicitly deferred.
- Critical inline scripts (theme prehydration) are an explicit exception — they MUST run synchronously before hydration.
- `defer` preserves order; `async` does not.
