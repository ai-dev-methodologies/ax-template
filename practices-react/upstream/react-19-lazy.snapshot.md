# react-19-lazy — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://react.dev/reference/react/lazy (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:48Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://react.dev/reference/react/lazy`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r077`
**Body SHA-256 (below the `---` divider, header excluded):** 930337355759959926296a1703e58dbdde467d0cc39419c8785ed22a0707cb2f

---

# Snapshot: React 19 — lazy()

- **source**: https://react.dev/reference/react/lazy
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19.2
- **via**: WebFetch

## What it does (verbatim)

> "Call lazy outside your components to declare a lazy-loaded React component"

```js
import { lazy } from 'react';
const MarkdownPreview = lazy(() => import('./MarkdownPreview.js'));
```

## Suspense required (verbatim)

> "Now that your component's code loads on demand, you also need to specify what should be displayed while it is loading. You can do this by wrapping the lazy component or any of its parents into a `<Suspense>` boundary"

## Critical caveat — module top-level only (verbatim)

> "Do NOT declare lazy components inside other components"

The doc shows the bad pattern explicitly with comments: "This will cause all state to be reset on re-renders".

## Module loading mechanic (verbatim)

> "This code relies on dynamic import(), which might require support from your bundler or framework."

## Audit implication

Vercel rules in the bundle-* family that present `next/dynamic` as the singular pattern are too Next-specific for a stack-agnostic catalog. React.lazy + Suspense is the portable equivalent and is GA across all bundlers since React 18.

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://react.dev/reference/react/lazy
HTTP status: 200 · extracted bytes: 7362 · sha256: 58d98075bd5a96cddc3f56d942004e1f52631dcaef44b0865719335b82f98414
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r077`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

lazy – React React v 19.2 Search ⌘ Ctrl K Learn Reference Community Blog react@19.2 Overview Hooks useActionState useCallback useContext useDebugValue useDeferredValue useEffect useEffectEvent useId useImperativeHandle useInsertionEffect useLayoutEffect useMemo useOptimistic useReducer useRef useState useSyncExternalStore useTransition Components <Fragment> (<>) <Profiler> <StrictMode> <Suspense> <Activity> <ViewTransition> - This feature is available in the latest Canary version of React APIs act addTransitionType - This feature is available in the latest Canary version of React cache cacheSignal captureOwnerStack createContext lazy memo startTransition use experimental_taintObjectReference - This feature is available in the latest Experimental version of React experimental_taintUniqueValue - This feature is available in the latest Experimental version of React react-dom@19.2 Hooks useFormStatus Components Common (e.g. <div>) <form> <input> <option> <progress> <select> <textarea> <link> <meta> <script> <style> <title> APIs createPortal flushSync preconnect prefetchDNS preinit preinitModule preload preloadModule Client APIs createRoot hydrateRoot Server APIs renderToPipeableStream renderToReadableStream renderToStaticMarkup renderToString resume resumeToPipeableStream Static APIs prerender prerenderToNodeStream resumeAndPrerender resumeAndPrerenderToNodeStream React Compiler Configuration compilationMode gating logger panicThreshold target Directives "use memo" "use no memo" Compiling Libraries React DevTools React Performance tracks eslint-plugin-react-hooks Lints exhaustive-deps rules-of-hooks component-hook-factories config error-boundaries gating globals immutability incompatible-library preserve-manual-memoization purity refs set-state-in-effect set-state-in-render static-components unsupported-syntax use-memo Rules of React Overview Components and Hooks must be pure React calls Components and Hooks Rules of Hooks React Server Components Server Components Server Functions Directives 'use client' 'use server' Legacy APIs Legacy React APIs Children cloneElement Component createElement createRef forwardRef isValidElement PureComponent API Reference APIs Copy page Copy lazy lazy lets you defer loading component’s code until it is rendered for the first time. const SomeComponent = lazy ( load ) Reference lazy(load) load function Usage Lazy-loading components with Suspense Troubleshooting My lazy component’s state gets reset unexpectedly Reference lazy(load) Call lazy outside your components to declare a lazy-loaded React component: import { lazy } from 'react' ; const MarkdownPreview = lazy ( ( ) => import ( './MarkdownPreview.js' ) ) ; See more examples below. Parameters load : A function that returns a Promise or another thenable (a Promise-like object with a then method). React will not call load until the first time you attempt to render the returned component. After React first calls load , it will wait for it to resolve, and then render the resolved value’s .default as a React component. Both the returned Promise and the Promise’s resolved value will be cached, so React will not call load more than once. If the Promise rejects, React will throw the rejection reason for the nearest Error Boundary to handle. Returns lazy returns a React component you can render in your tree. While the code for the lazy component is still loading, attempting to render it will suspend. Use <Suspense> to display a loading indicator while it’s loading. load function Parameters load receives no parameters. Returns You need to return a Promise or some other thenable (a Promise-like object with a then method). It needs to eventually resolve to an object whose .default property is a valid React component type, such as a function, memo , or a forwardRef component. Usage Lazy-loading components with Suspense Usually, you import components with the static import declaration: import MarkdownPreview from './MarkdownPreview.js' ; To defer loading this component’s code until it’s rendered for the first time, replace this import with: import { lazy } from 'react' ; const MarkdownPreview = lazy ( ( ) => import ( './MarkdownPreview.js' ) ) ; This code relies on dynamic import() , which might require support from your bundler or framework. Using this pattern requires that the lazy component you’re importing was exported as the default export. Now that your component’s code loads on demand, you also need to specify what should be displayed while it is loading. You can do this by wrapping the lazy component or any of its parents into a <Suspense> boundary: < Suspense fallback = { < Loading /> } > < h2 > Preview </ h2 > < MarkdownPreview /> </ Suspense > In this example, the code for MarkdownPreview won’t be loaded until you attempt to render it. If MarkdownPreview hasn’t loaded yet, Loading will be shown in its place. Try ticking the checkbox: App.js Loading.js MarkdownPreview.js App.js Reload Clear Fork import { useState , Suspense , lazy } from 'react' ; import Loading from './Loading.js' ; const MarkdownPreview = lazy ( ( ) => delayForDemo ( import ( './MarkdownPreview.js' ) ) ) ; export default function MarkdownEditor ( ) { const [ showPreview , setShowPreview ] = useState ( false ) ; const [ markdown , setMarkdown ] = useState ( 'Hello, **world**!' ) ; return ( < > < textarea value = { markdown } onChange = { e => setMarkdown ( e . target . value ) } /> < label > < input type = "checkbox" checked = { showPreview } onChange = { e => setShowPreview ( e . target . checked ) } /> Show preview </ label > < hr /> { showPreview && ( < Suspense fallback = { < Loading /> } > < h2 > Preview </ h2 > < MarkdownPreview markdown = { markdown } /> </ Suspense > ) } </ > ) ; } // Add a fixed delay so you can see the loading state function delayForDemo ( promise ) { return new Promise ( resolve => { setTimeout ( resolve , 2000 ) ; } ) . then ( ( ) => promise ) ; } Show more This demo loads with an artificial delay. The next time you untick and tick the checkbox, Preview will be cached, so there will be no loading state. To see the loading state again, click “Reset” on the sandbox. Learn more about managing loading states with Suspense. Troubleshooting My lazy component’s state gets reset unexpectedly Do not declare lazy components inside other components: import { lazy } from 'react' ; function Editor ( ) { // 🔴 Bad: This will cause all state to be reset on re-renders const MarkdownPreview = lazy ( ( ) => import ( './MarkdownPreview.js' ) ) ; // ... } Instead, always declare them at the top level of your module: import { lazy } from 'react' ; // ✅ Good: Declare lazy components outside of your components const MarkdownPreview = lazy ( ( ) => import ( './MarkdownPreview.js' ) ) ; function Editor ( ) { // ... } Previous createContext Next memo Copyright © Meta Platforms, Inc no uwu plz uwu? Logo by @sawaratsuki1004 Learn React Quick Start Installation Describing the UI Adding Interactivity Managing State Escape Hatches API Reference React APIs React DOM APIs Community Code of Conduct Meet the Team Docs Contributors Acknowledgements More Blog React Native Privacy Terms On this page Overview Reference lazy(load) load function Usage Lazy-loading components with Suspense Troubleshooting My lazy component’s state gets reset unexpectedly
