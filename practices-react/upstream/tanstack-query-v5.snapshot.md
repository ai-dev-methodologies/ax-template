# tanstack-query-v5 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://tanstack.com/query/latest/docs/framework/react/overview (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:57Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://tanstack.com/query/latest/docs/framework/react/overview`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r094`
**Body SHA-256 (below the `---` divider, header excluded):** 76ca48c23bd01fc666a212e8971e96069b704a81269c9a1c589f315cf1deca73

---

---
snapshot_id: tanstack-query-v5
source: "https://tanstack.com/query/latest/docs/framework/react/overview"
fetched_at: "2026-05-17T13:00:00Z"
version_observed: "tanstack-query@5.x"
via: WebFetch
sha: "f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9"
---

# TanStack Query v5 — React Overview

Source: https://tanstack.com/query/latest/docs/framework/react/overview  
Fetched: 2026-05-17

## Overview

> "TanStack Query makes fetching, caching, synchronizing and updating async state trivial."

TanStack Query (previously React Query) addresses managing **server state** — which exists
remotely, requires async operations, can change without your knowledge, and risks becoming outdated.

## Problems it solves

Without TanStack Query, developers manually handle:
- Caching complexity
- Deduplicating redundant requests
- Background data refreshing
- Memory management and garbage collection
- Pagination, infinite scrolling
- Optimistic updates and rollbacks

## Key Benefits

- **Code Reduction**: Eliminates complex boilerplate — replace multiple `useState` + `useEffect` patterns with `useQuery`.
- **Enhanced Maintainability**: Adding new server state sources requires minimal wiring.
- **User Experience**: Makes interfaces "faster and more responsive than ever before."
- **Resource Efficiency**: Intelligent caching reduces bandwidth consumption.

## Basic Pattern (v5)

```tsx
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query'

const queryClient = new QueryClient()

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Posts />
    </QueryClientProvider>
  )
}

function Posts() {
  const { data, isPending, isError } = useQuery({
    queryKey: ['posts'],
    queryFn: () => fetch('/api/posts').then(r => r.json()),
  })

  if (isPending) return <div>Loading...</div>
  if (isError) return <div>Error</div>
  return <ul>{data.map(p => <li key={p.id}>{p.title}</li>)}</ul>
}
```

## v5 Key Changes from v4

- Single unified `useQuery` signature (no overloads).
- First-class React 19 Suspense support via `useSuspenseQuery`.
- `status: 'pending'` replaces `status: 'loading'`.
- `fetchStatus` added to distinguish between background fetching and initial load.

## When to Use vs Zustand

| Concern | Tool |
|---------|------|
| Server state (API data) | TanStack Query |
| Client state (UI, auth token, theme) | Zustand |
| Form state | React Hook Form |
| URL state | search params / route segments |

Do NOT duplicate server state into Zustand stores.

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://tanstack.com/query/latest/docs/framework/react/overview
HTTP status: 200 · extracted bytes: 7388 · sha256: ae2ef23f85b2f407c77ccebce1401d7b60d963195ce49967a66025bb22b39398
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r094`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Overview | TanStack Query React Docs Libraries Framework Start Router Data & State Query DB Store AI UI & UX Table Charts Form Hotkeys Markdown Highlight Performance Virtual Pacer Tooling Devtools Config CLI Intent Browse all libraries Blog Blog & Release Notes About YouTube The official TanStack channel. Workshops Live sessions from the maintainers. Release Notes The latest releases and changelog. Community Channels Discord Real-time community support. GitHub Source, issues, discussions, and releases. People & Work Maintainers Meet the people maintaining the stack. Contributors Core, library, and community contributors. Showcase Teams building with TanStack. Tools Tools Builder Alpha Generate TanStack app starters. Stats NPM and ecosystem usage data. Merch View all Support Support Support Overview Find the right support path. Partners Companies supporting TanStack. OSS Sponsors Sponsors keeping TanStack open source. Enterprise Support Private consulting and expert support. Contact Get in touch with the TanStack team. About Ethos How we approach open source. Tenets The values that shape TanStack libraries. Design System Logos, tokens, and UI components. Partners Sponsorships, placements, and partner pages. Work with Partnership Inquiry Search AI Ask AI Log In Log In Libraries Blog Blog & Release Notes About YouTube The official TanStack channel. Workshops Live sessions from the maintainers. Release Notes The latest releases and changelog. Community Channels Discord Real-time community support. GitHub Source, issues, discussions, and releases. People & Work Maintainers Meet the people maintaining the stack. Contributors Core, library, and community contributors. Showcase Teams building with TanStack. Tools Tools Builder Alpha Generate TanStack app starters. Stats NPM and ecosystem usage data. Merch View all Support Support Support Overview Find the right support path. Partners Companies supporting TanStack. OSS Sponsors Sponsors keeping TanStack open source. Enterprise Support Private consulting and expert support. Contact Get in touch with the TanStack team. About Ethos How we approach open source. Tenets The values that shape TanStack libraries. Design System Logos, tokens, and UI components. Partnership Inquiry Query Docs React Latest Search... K Home Get Started Guides API Examples Getting Started Overview Installation Quick Start Devtools Comparison TypeScript GraphQL React Native Query Menu Menu Home Get Started Guides API Examples React Latest Getting Started Overview Installation Quick Start Devtools Comparison TypeScript GraphQL React Native AI/LLM: This documentation page is available in plain markdown format at /query/latest/docs/framework/react/overview .md Getting Started On this page Overview Copy page TanStack Query (formerly known as React Query) is often described as the missing data-fetching library for web applications, but in more technical terms, it makes fetching, caching, synchronizing and updating server state in your web applications a breeze. Motivation # Most core web frameworks do not come with an opinionated way of fetching or updating data in a holistic way. Because of this developers end up building either meta-frameworks which encapsulate strict opinions about data-fetching, or they invent their own ways of fetching data. This usually means cobbling together component-based state and side-effects, or using more general purpose state management libraries to store and provide asynchronous data throughout their apps. While most traditional state management libraries are great for working with client state, they are not so great at working with async or server state . This is because server state is totally different . For starters, server state: Is persisted remotely in a location you may not control or own Requires asynchronous APIs for fetching and updating Implies shared ownership and can be changed by other people without your knowledge Can potentially become "out of date" in your applications if you're not careful Once you grasp the nature of server state in your application, even more challenges will arise as you go, for example: Caching... (possibly the hardest thing to do in programming) Deduping multiple requests for the same data into a single request Updating "out of date" data in the background Knowing when data is "out of date" Reflecting updates to data as quickly as possible Performance optimizations like pagination and lazy loading data Managing memory and garbage collection of server state Memoizing query results with structural sharing If you're not overwhelmed by that list, then that must mean that you've probably solved all of your server state problems already and deserve an award. However, if you are like a vast majority of people, you either have yet to tackle all or most of these challenges and we're only scratching the surface! TanStack Query is hands down one of the best libraries for managing server state. It works amazingly well out-of-the-box, with zero-config, and can be customized to your liking as your application grows. TanStack Query allows you to defeat and overcome the tricky challenges and hurdles of server state and control your app data before it starts to control you. On a more technical note, TanStack Query will likely: Help you remove many lines of complicated and misunderstood code from your application and replace with just a handful of lines of TanStack Query logic Make your application more maintainable and easier to build new features without worrying about wiring up new server state data sources Have a direct impact on your end-users by making your application feel faster and more responsive than ever before Potentially help you save on bandwidth and increase memory performance Enough talk, show me some code already! # In the example below, you can see TanStack Query in its most basic and simple form being used to fetch the GitHub stats for the TanStack Query GitHub project itself: Open in StackBlitz tsx import { QueryClient , QueryClientProvider , useQuery, } from '@tanstack/react-query' const queryClient = new QueryClient () export default function App () { return ( < QueryClientProvider client ={queryClient}> < Example /> </ QueryClientProvider > ) } function Example () { const { isPending, error, data } = useQuery ({ queryKey: [ 'repoData' ], queryFn: () => fetch ( 'https://api.github.com/repos/TanStack/query' ). then ((res) => res. json (), ), }) if (isPending) return 'Loading...' if (error) return 'An error has occurred: ' + error. message return ( < div > < h1 >{data. name }</ h1 > < p >{data. description }</ p > < strong >👀 {data. subscribers_count }</ strong >{ ' ' } < strong >✨ {data. stargazers_count }</ strong >{ ' ' } < strong >🍴 {data. forks_count }</ strong > </ div > ) } You talked me into it, so what now? # Consider taking the official TanStack Query Course (or buying it for your whole team!) Learn TanStack Query at your own pace with our amazingly thorough Walkthrough Guide and API Reference See the Article Why You Want React Query . Edit on GitHub Next Installation On this page Motivation Enough talk, show me some code already! You talked me into it, so what now? Blog @Tan_Stack on X.com @TannerLinsley on X.com GitHub YouTube Ethos Tenets Privacy Policy Terms of Service © 2026 TanStack LLC Partners Become a Partner Gold Silver Bronze Latest Posts
