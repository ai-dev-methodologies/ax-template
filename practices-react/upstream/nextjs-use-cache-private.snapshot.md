# nextjs-use-cache-private — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://nextjs.org/docs/app/api-reference/directives/use-cache-private (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:52Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://nextjs.org/docs/app/api-reference/directives/use-cache-private`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r087`
**Body SHA-256 (below the `---` divider, header excluded):** 1e286cbc50c92e1c893716baccdf3e2bc787d4a81f54e5787476511b8cd0f4f5

---

# Snapshot: Next.js 16 — 'use cache: private'

- **source**: https://nextjs.org/docs/app/api-reference/directives/use-cache-private
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13
- **version_observed**: 16.2.6
- **via**: WebFetch

## Status (verbatim)

> "This feature is currently experimental and subject to change, it's not recommended for production."

> "This directive is marked as experimental because it depends on runtime prefetching, which is not yet stable."

Version history: introduced in v16.0.0 with Cache Components.

## What it does (verbatim)

> "The 'use cache: private' directive allows functions to access runtime request APIs like cookies(), headers(), and searchParams within a cached scope. However, results are never stored on the server, they're cached only in the browser's memory and do not persist across page reloads."

## When to use (verbatim)

> "Reach for 'use cache: private' when:
> - You want to cache a function that already accesses runtime data, and refactoring to move the runtime access outside and pass values as arguments is not practical.
> - Compliance requirements prevent storing certain data on the server, even temporarily"

## API allowed inside (verbatim table)

| API | use cache | use cache: private |
|---|---|---|
| cookies() | No | **Yes** |
| headers() | No | **Yes** |
| searchParams | No | **Yes** |
| connection() | No | No |

## Critical constraints (verbatim)

- "Because this directive accesses runtime data, the function executes on every server render and is excluded from running during static shell generation."
- "It is **not** possible to configure custom cache handlers for 'use cache: private'."
- "This directive is not available in Route Handlers."
- "The stale time must be at least 30 seconds for runtime prefetching to work."

## Cache scope

Per-client (browser memory only). Does not persist across page reloads. Never written server-side.

## Audit implication

`'use cache: private'` is the **escape hatch** when you legitimately cannot refactor runtime data out of a cached scope. It is:
- experimental in 16.2.6 (not production-recommended),
- per-client (no cross-user sharing),
- non-persistent (lost on reload),
- not allowed in Route Handlers,
- not custom-handler-configurable.

A catalog rule must lead with: "first try to refactor; this is the fallback."

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://nextjs.org/docs/app/api-reference/directives/use-cache-private
HTTP status: 200 · extracted bytes: 8956 · sha256: f7701769588ab00da69083beb12a989ae8321be68696997244f6694652e9860b
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r087`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Directives: use cache: private | Next.js Skip to content Search documentation... ⌘K Search... ⌘K Showcase Docs Blog Templates Enterprise Search documentation... ⌘K Search... ⌘K Feedback Learn Menu Using App Router Features available in /app Latest Version 16.2.12 Getting Started Installation Project Structure Layouts and Pages Linking and Navigating Server and Client Components Fetching Data Mutating Data Caching Revalidating Error Handling CSS Image Optimization Font Optimization Metadata and OG images Route Handlers Proxy Deploying Upgrading Guides AI Coding Agents Analytics Authentication Backend for Frontend Caching (Previous Model) CDN Caching CI Build Caching Content Security Policy CSS-in-JS Custom Server Data Security Debugging Deploying to Platforms Draft Mode Environment Variables Forms How Revalidation Works ISR Instrumentation Internationalization JSON-LD Lazy Loading Development Environment Next.js MCP Server MDX Memory Usage Migrating App Router Create React App Vite Migrating to Cache Components Multi-tenant Multi-zones OpenTelemetry Package Bundling PPR Platform Guide Prefetching Preserving UI state Preventing Flash Production PWAs Public pages Redirecting Rendering Philosophy Sass Scripts Self-Hosting Server Actions SPAs Static Exports Streaming Tailwind CSS v3 Testing Cypress Jest Playwright Vitest Third Party Libraries Upgrading Codemods Version 14 Version 15 Version 16 Videos View transitions API Reference Directives use cache use cache: private use cache: remote use client use server Components Font Form Component Image Component Link Component Script Component File-system conventions default.js Dynamic Segments error.js forbidden.js instrumentation.js instrumentation-client.js Intercepting Routes layout.js loading.js mdx-components.js not-found.js page.js Parallel Routes proxy.js public route.js Route Groups src template.js unauthorized.js Metadata Files favicon, icon, and apple-icon manifest.json opengraph-image and twitter-image robots.txt sitemap.xml Route Segment Config dynamicParams maxDuration preferredRegion runtime Functions after cacheLife cacheTag unstable_catchError connection cookies draftMode fetch forbidden generateImageMetadata generateMetadata generateSitemaps generateStaticParams generateViewport headers ImageResponse NextRequest NextResponse notFound permanentRedirect redirect refresh revalidatePath revalidateTag unauthorized unstable_cache unstable_noStore unstable_rethrow updateTag useLinkStatus useParams usePathname useReportWebVitals useRouter useSearchParams useSelectedLayoutSegment useSelectedLayoutSegments userAgent Configuration next.config.js adapterPath allowedDevOrigins appDir assetPrefix authInterrupts basePath cacheComponents cacheHandlers cacheLife compress crossOrigin cssChunking deploymentId devIndicators distDir env expireTime exportPathMap generateBuildId generateEtags headers htmlLimitedBots httpAgentOptions images cacheHandler inlineCss logging mdxRs onDemandEntries optimizePackageImports output pageExtensions poweredByHeader productionBrowserSourceMaps proxyClientMaxBodySize reactCompiler reactMaxHeadersLength reactStrictMode redirects rewrites sassOptions serverActions serverComponentsHmrCache serverExternalPackages staleTimes staticGeneration* taint trailingSlash transpilePackages turbopack turbopackFileSystemCache turbopack.ignoreIssue turbopackLocalPostcssConfig typedRoutes typescript urlImports useLightningcss useTypeScriptCli viewTransition webpack webVitalsAttribution TypeScript ESLint CLI create-next-app next CLI Adapters Configuration Creating an Adapter API Reference Testing Adapters Routing with @next/routing Implementing PPR in an Adapter Runtime Integration Invoking Entrypoints Output Types Routing Information Use Cases Edge Runtime Turbopack Glossary Architecture Accessibility Fast Refresh Next.js Compiler Supported Browsers Community Contribution Guide Rspack On this page Usage Basic example Request APIs allowed in private caches Version History Related Edit this page on GitHub Scroll to top This page is also available as Markdown at /docs/app/api-reference/directives/use-cache-private.md . For an index of Next.js documentation , see /docs/llms.txt . API Reference Directives use cache: private Copy page use cache: private This feature is currently experimental and subject to change, it's not recommended for production. Try it out and share your feedback on GitHub . Last updated March 3, 2026 The 'use cache: private' directive allows functions to access runtime request APIs like cookies() , headers() , and searchParams within a cached scope. However, results are never stored on the server , they're cached only in the browser's memory and do not persist across page reloads. Reach for 'use cache: private' when: You want to cache a function that already accesses runtime data, and refactoring to move the runtime access outside and pass values as arguments is not practical. Compliance requirements prevent storing certain data on the server, even temporarily Because this directive accesses runtime data, the function executes on every server render and is excluded from running during static shell generation. It is not possible to configure custom cache handlers for 'use cache: private' . For a comparison of the different cache directives, see How use cache: remote differs from use cache and use cache: private . Good to know : This directive is marked as experimental because it depends on runtime prefetching, which is not yet stable. Runtime prefetching is an upcoming feature that will let the router prefetch past the static shell into any cached scope, not just private caches. Usage To use 'use cache: private' , enable the cacheComponents flag in your next.config.ts file: next.config.ts TypeScript JavaScript TypeScript import type { NextConfig } from 'next' const nextConfig : NextConfig = { cacheComponents : true , } export default nextConfig Then add 'use cache: private' to your function along with a cacheLife configuration. Good to know : This directive is not available in Route Handlers. Basic example In this example, we demonstrate that you can access cookies within a 'use cache: private' scope: app/product/[id]/page.tsx TypeScript JavaScript TypeScript import { Suspense } from 'react' import { cookies } from 'next/headers' import { cacheLife , cacheTag } from 'next/cache' export async function generateStaticParams () { return [{ id : '1' }] } export default async function ProductPage ({ params , } : { params : Promise <{ id : string }> }) { const { id } = await params return ( < div > < ProductDetails id = {id} /> < Suspense fallback = {< div >Loading recommendations...</ div >}> < Recommendations productId = {id} /> </ Suspense > </ div > ) } async function Recommendations ({ productId } : { productId : string }) { const recommendations = await getRecommendations (productId) return ( < div > { recommendations .map ((rec) => ( < ProductCard key = { rec .id} product = {rec} /> ))} </ div > ) } async function getRecommendations (productId : string ) { 'use cache: private' cacheTag ( `recommendations- ${ productId } ` ) cacheLife ({ stale : 60 }) // Access cookies within private cache functions const sessionId = ( await cookies ()) .get ( 'session-id' )?.value || 'guest' return getPersonalizedRecommendations (productId , sessionId) } Good to know : The stale time must be at least 30 seconds for runtime prefetching to work. See cacheLife client cache behavior for details. Request APIs allowed in private caches The following request-specific APIs can be used inside 'use cache: private' functions: API Allowed in use cache Allowed in 'use cache: private' cookies() No Yes headers() No Yes searchParams No Yes connection() No No Note: The connection() API is prohibited in both use cache and 'use cache: private' as it provides connection-specific information that cannot be safely cached. Version History Version Changes v16.0.0 "use cache: private" is enabled with the Cache Components feature. Related View related API references. use cache Learn how to use the "use cache" directive to cache data in your Next.js application. cacheComponents Learn how to enable the cacheComponents flag in Next.js. cacheLife Learn how to use the cacheLife function to set the cache expiration time for a cached function or component. cacheTag Learn how to use the cacheTag function to manage cache invalidation in your Next.js application. Previous use cache Next use cache: remote Was this helpful? supported. Send Resources Docs Support Policy Learn Showcase Blog Team Analytics Next.js Conf Previews Evals More Next.js Commerce Contact Sales Community GitHub Releases Telemetry Governance Ecosystem Working Group About Vercel Next.js + Vercel Open Source Software GitHub Bluesky X Legal Privacy Policy Cookie Preferences Subscribe to our newsletter Stay updated on new releases and features, guides, and case studies. Subscribe © 2026 Vercel, Inc.
