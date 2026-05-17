# Upstream Snapshot — React 19 Error Boundaries

**id:** react-19-error-boundary  
**source:** https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary  
**fetched_at:** 2026-05-18T00:00:00Z  
**via:** WebFetch  
**tier:** 2  
**version_observed:** react@19.2

---

## Overview

Error boundaries are React class components that catch JavaScript errors anywhere in their
child component tree. They log the errors and display a fallback UI instead of the crashed
component tree.

> "Error boundaries are React class components that let you display some fallback UI instead
> of the component tree that crashed. They catch errors during rendering, in lifecycle
> methods, and in constructors of any components below them in the tree."

## Required lifecycle methods

```tsx
class ErrorBoundary extends React.Component<Props, State> {
  static getDerivedStateFromError(error: Error): State {
    // Update state so the next render will show the fallback UI.
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    // Report to error logging service
    logErrorToMyService(error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback
    }
    return this.props.children
  }
}
```

## Reset key pattern

A common pattern to reset an error boundary is to change a `key` prop. When `key` changes,
React remounts the subtree from scratch, clearing the error state:

```tsx
// Parent controls recovery by incrementing resetKey
<ErrorBoundary key={resetKey} fallback={<ErrorFallback />}>
  <SomeComponent />
</ErrorBoundary>
```

## getDerivedStateFromError vs componentDidCatch

- **`getDerivedStateFromError`**: Called during the *render* phase. Used to update state so
  the next render shows the fallback UI. Must be a pure function with no side effects.
- **`componentDidCatch`**: Called during the *commit* phase. Used for side effects like
  logging errors to an external service.

## React 19 notes

- `reportError()` global function now available for manual error reporting
- Error boundaries remain class-based in React 19 (no hook equivalent for catching render errors)
- `useErrorBoundary()` hooks from third-party libs wrap this class API

## Limitations

Error boundaries do NOT catch errors in:
- Event handlers (use try/catch directly)
- Asynchronous code (setTimeout, Promise rejections)
- Server-side rendering
- Errors thrown in the error boundary itself
