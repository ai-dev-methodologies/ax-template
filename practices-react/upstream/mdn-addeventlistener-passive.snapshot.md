# Snapshot: MDN — EventTarget.addEventListener (passive option)

- **source**: https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener
- **role**: primitive-semantics
- **fetched_at**: 2026-05-16T00:00:00Z
- **via**: WebFetch
- **section**: #passive

## passive option (verbatim)

> "A boolean value that, if true, indicates that the function specified by listener will never call preventDefault(). If a passive listener does call preventDefault(), the user agent will do nothing other than generate a console warning."

> "If not specified, defaults to false – except that in browsers other than Safari, defaults to true for the wheel, mousewheel, touchstart and touchmove events. See Using passive listeners to learn more."

## Why this matters

The browser needs to know whether your listener will cancel the action (scroll, zoom) to decide if it can start the action immediately or must wait for the listener. `passive: true` removes the waiting.

## Audit implication

Modern Chrome/Firefox default `touchstart`, `touchmove`, `wheel`, `mousewheel` to passive when listener target is the root (window/document). Custom scroll containers and other engines still need explicit options. Explicit options are always correct and remove ambiguity.
