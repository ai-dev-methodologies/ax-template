# Snapshot: React 19.2 — Activity

- **source**: https://react.dev/reference/react/Activity
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19.2 (stable Oct 2025)
- **via**: WebSearch synthesis

## Status

Stable in React 19.2 (released Oct 2025). No longer experimental.

## Modes (verbatim)

> "Visible shows the children, mounts effects, and allows updates to be processed normally."

> "Hidden hides the children, unmounts effects, and defers all updates until React has nothing left to work on."

## Use case (verbatim)

> "You can use Activity to render hidden parts of the app that a user is likely to navigate to next, or to save the state of parts the user navigates away from."

> "This helps make navigations quicker by loading data, css, and images in the background, and allows back navigations to maintain state such as input fields."

## Future plans (verbatim)

> "The Activity component is a new primitive and the React team plans to extend it with additional modes in the future for more use cases."

## Audit implication

Hidden mode **unmounts effects** — the Vercel rule didn't surface this. Catalog rule must say so explicitly, or developers will rely on Activity for subscriptions that get torn down.
