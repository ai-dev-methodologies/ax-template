# Snapshot: React docs — You Might Not Need an Effect (initializing the application)

- **source**: https://react.dev/learn/you-might-not-need-an-effect
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19.2
- **via**: WebFetch

## The problem (verbatim)

> "Effects with logic that should only ever run once
> useEffect(() => {
>   loadDataFromLocalStorage();
>   checkAuthToken();
> }, []);
>
> However, you'll quickly discover that it runs twice in development. This can cause issues — for example, maybe it invalidates the authentication token because the function wasn't designed to be called twice."

## Recommended Solution 1 — module-level didInit guard (verbatim)

```js
let didInit = false;

function App() {
  useEffect(() => {
    if (!didInit) {
      didInit = true;
      loadDataFromLocalStorage();
      checkAuthToken();
    }
  }, []);
}
```

## Recommended Solution 2 — module-level init with browser check (verbatim)

```js
if (typeof window !== 'undefined') {
  checkAuthToken();
  loadDataFromLocalStorage();
}

function App() {
  // ...
}
```

## Caveat (verbatim)

> "Code at the top level runs once when your component is imported — even if it doesn't end up being rendered. To avoid slowdown or surprising behavior when importing arbitrary components, don't overuse this pattern. Keep app-wide initialization logic to root component modules like App.js or in your application's entry point."

## Audit implication

The didInit pattern in Vercel's advanced-init-once rule is React-docs-canonical.
The module-init alternative is equally official and often cleaner. Catalog rule
should present both.
