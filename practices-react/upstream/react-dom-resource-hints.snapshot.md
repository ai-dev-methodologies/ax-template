# react-dom-resource-hints — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://react.dev/reference/react-dom (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:50Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://react.dev/reference/react-dom`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r082`
**Body SHA-256 (below the `---` divider, header excluded):** f0022e67df7532755eb61185fe227f82fd209ebec9af96c3f58ff7f75c04c47f

---

# Snapshot: React DOM — Resource Preloading APIs (React 19 stable)

- **source**: https://react.dev/reference/react-dom
- **role**: canonical-react
- **fetched_at**: 2026-05-16T00:00:00Z
- **react_version_observed**: 19 (stable)
- **via**: web-derived from React docs index

## APIs (verbatim names)

- `prefetchDNS(href)` — DNS resolution only
- `preconnect(href)` — DNS + TCP + TLS
- `preload(href, options)` — fetch a resource (font/style/script/image)
- `preloadModule(href, options)` — fetch an ES module
- `preinit(href, options)` — fetch AND execute a stylesheet/script
- `preinitModule(href, options)` — fetch AND execute an ES module

## When to use which (per React DOM table)

| API | Use case |
|---|---|
| prefetchDNS | Third-party domains you'll connect to later |
| preconnect | APIs/CDNs you'll fetch from immediately |
| preload | Critical resources needed for current page |
| preloadModule | JS modules for likely next navigation |
| preinit | Stylesheets/scripts that must execute early |
| preinitModule | ES modules that must execute early |

## Usage shape

Callable from Server Components, Client Components, layouts. Especially powerful from Server Components — the hint can be in the HTML before the client even receives the document.

## Audit implication

These are stable React 19 APIs. Catalog rule should require Server Component / layout use site for max benefit. Overuse penalty: hinting too many origins or preloading too many resources competes for the very bandwidth being optimized.

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://react.dev/reference/react-dom
HTTP status: 200 · extracted bytes: 4616 · sha256: ecee6168f9cfe227272e03212fec5f5c29c7dbffe637bc247914762437bf9d0f
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r082`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

React DOM APIs – React React v 19.2 Search ⌘ Ctrl K Learn Reference Community Blog react@19.2 Overview Hooks useActionState useCallback useContext useDebugValue useDeferredValue useEffect useEffectEvent useId useImperativeHandle useInsertionEffect useLayoutEffect useMemo useOptimistic useReducer useRef useState useSyncExternalStore useTransition Components <Fragment> (<>) <Profiler> <StrictMode> <Suspense> <Activity> <ViewTransition> - This feature is available in the latest Canary version of React APIs act addTransitionType - This feature is available in the latest Canary version of React cache cacheSignal captureOwnerStack createContext lazy memo startTransition use experimental_taintObjectReference - This feature is available in the latest Experimental version of React experimental_taintUniqueValue - This feature is available in the latest Experimental version of React react-dom@19.2 Hooks useFormStatus Components Common (e.g. <div>) <form> <input> <option> <progress> <select> <textarea> <link> <meta> <script> <style> <title> APIs createPortal flushSync preconnect prefetchDNS preinit preinitModule preload preloadModule Client APIs createRoot hydrateRoot Server APIs renderToPipeableStream renderToReadableStream renderToStaticMarkup renderToString resume resumeToPipeableStream Static APIs prerender prerenderToNodeStream resumeAndPrerender resumeAndPrerenderToNodeStream React Compiler Configuration compilationMode gating logger panicThreshold target Directives "use memo" "use no memo" Compiling Libraries React DevTools React Performance tracks eslint-plugin-react-hooks Lints exhaustive-deps rules-of-hooks component-hook-factories config error-boundaries gating globals immutability incompatible-library preserve-manual-memoization purity refs set-state-in-effect set-state-in-render static-components unsupported-syntax use-memo Rules of React Overview Components and Hooks must be pure React calls Components and Hooks Rules of Hooks React Server Components Server Components Server Functions Directives 'use client' 'use server' Legacy APIs Legacy React APIs Children cloneElement Component createElement createRef forwardRef isValidElement PureComponent API Reference Copy page Copy React DOM APIs The react-dom package contains methods that are only supported for the web applications (which run in the browser DOM environment). They are not supported for React Native. APIs These APIs can be imported from your components. They are rarely used: createPortal lets you render child components in a different part of the DOM tree. flushSync lets you force React to flush a state update and update the DOM synchronously. Resource Preloading APIs These APIs can be used to make apps faster by pre-loading resources such as scripts, stylesheets, and fonts as soon as you know you need them, for example before navigating to another page where the resources will be used. React-based frameworks frequently handle resource loading for you, so you might not have to call these APIs yourself. Consult your framework’s documentation for details. prefetchDNS lets you prefetch the IP address of a DNS domain name that you expect to connect to. preconnect lets you connect to a server you expect to request resources from, even if you don’t know what resources you’ll need yet. preload lets you fetch a stylesheet, font, image, or external script that you expect to use. preloadModule lets you fetch an ESM module that you expect to use. preinit lets you fetch and evaluate an external script or fetch and insert a stylesheet. preinitModule lets you fetch and evaluate an ESM module. Entry points The react-dom package provides two additional entry points: react-dom/client contains APIs to render React components on the client (in the browser). react-dom/server contains APIs to render React components on the server. Removed APIs These APIs were removed in React 19: findDOMNode : see alternatives . hydrate : use hydrateRoot instead. render : use createRoot instead. unmountComponentAtNode : use root.unmount() instead. renderToNodeStream : use react-dom/server APIs instead. renderToStaticNodeStream : use react-dom/server APIs instead. Previous <title> Next createPortal Copyright © Meta Platforms, Inc no uwu plz uwu? Logo by @sawaratsuki1004 Learn React Quick Start Installation Describing the UI Adding Interactivity Managing State Escape Hatches API Reference React APIs React DOM APIs Community Code of Conduct Meet the Team Docs Contributors Acknowledgements More Blog React Native Privacy Terms On this page Overview APIs Resource Preloading APIs Entry points Removed APIs
