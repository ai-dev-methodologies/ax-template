# nextjs-lazy-loading — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://nextjs.org/docs/app/guides/lazy-loading (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:48Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://nextjs.org/docs/app/guides/lazy-loading`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r078`
**Body SHA-256 (below the `---` divider, header excluded):** 4838883d4dcf5d03e4f56d11f4ef5bdaae84957c2cc6d64e8a41dd52cedaf35b

---

# Snapshot: Next.js 16 — Lazy Loading guide

- **source**: https://nextjs.org/docs/app/guides/lazy-loading
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13
- **version_observed**: 16.2.6
- **via**: WebFetch

## Two patterns (verbatim)

> "There are two ways you can implement lazy loading in Next.js:
> 1. Using Dynamic Imports with `next/dynamic`
> 2. Using `React.lazy()` with Suspense"

## next/dynamic is composite of React.lazy + Suspense (verbatim)

> "`next/dynamic` is a composite of `React.lazy()` and Suspense. It behaves the same way in the app and pages directories to allow for incremental migration."

## Server vs Client Component lazy semantics (verbatim)

> "By default, Server Components are automatically code split, and you can use streaming to progressively send pieces of UI from the server to the client. Lazy loading applies to Client Components."

> "When a Server Component dynamically imports a Client Component, automatic code splitting is currently NOT supported."

## ssr:false constraint (verbatim)

> "`ssr: false` option will only work for Client Components, move it into Client Components ensure the client code-splitting working properly."

> "`ssr: false` is not allowed with `next/dynamic` in Server Components. Please move it into a Client Component."

## External library on-demand (verbatim)

> "External libraries can be loaded on demand using the import() function."

Example: `const Fuse = (await import('fuse.js')).default` inside an event handler.

## Magic comments

- `webpackIgnore` / `turbopackIgnore` — skip bundling, runtime-only modules.
- `turbopackOptional` — suppress build errors when a module might not exist.
- Magic comments do NOT work with static `import` statements.

## Audit implication

Bundle-family rules should:
1. Distinguish Client Component lazy (the target) from Server Component (already auto-split).
2. Use React.lazy + Suspense as the portable default; next/dynamic when Next-specific features are needed.
3. Recognize that external SDK module loading is via bare `import()`, not via `next/dynamic` (which is for components).

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://nextjs.org/docs/app/guides/lazy-loading
HTTP status: 200 · extracted bytes: 11109 · sha256: 328d6e7537cb10bf917c6b08ee987a0c184f07007f938098b0578c149f5a488d
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r078`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Guides: Lazy Loading | Next.js Skip to content Search documentation... ⌘K Search... ⌘K Showcase Docs Blog Templates Enterprise Search documentation... ⌘K Search... ⌘K Feedback Learn Menu Using App Router Features available in /app Latest Version 16.2.12 Getting Started Installation Project Structure Layouts and Pages Linking and Navigating Server and Client Components Fetching Data Mutating Data Caching Revalidating Error Handling CSS Image Optimization Font Optimization Metadata and OG images Route Handlers Proxy Deploying Upgrading Guides AI Coding Agents Analytics Authentication Backend for Frontend Caching (Previous Model) CDN Caching CI Build Caching Content Security Policy CSS-in-JS Custom Server Data Security Debugging Deploying to Platforms Draft Mode Environment Variables Forms How Revalidation Works ISR Instrumentation Internationalization JSON-LD Lazy Loading Development Environment Next.js MCP Server MDX Memory Usage Migrating App Router Create React App Vite Migrating to Cache Components Multi-tenant Multi-zones OpenTelemetry Package Bundling PPR Platform Guide Prefetching Preserving UI state Preventing Flash Production PWAs Public pages Redirecting Rendering Philosophy Sass Scripts Self-Hosting Server Actions SPAs Static Exports Streaming Tailwind CSS v3 Testing Cypress Jest Playwright Vitest Third Party Libraries Upgrading Codemods Version 14 Version 15 Version 16 Videos View transitions API Reference Directives use cache use cache: private use cache: remote use client use server Components Font Form Component Image Component Link Component Script Component File-system conventions default.js Dynamic Segments error.js forbidden.js instrumentation.js instrumentation-client.js Intercepting Routes layout.js loading.js mdx-components.js not-found.js page.js Parallel Routes proxy.js public route.js Route Groups src template.js unauthorized.js Metadata Files favicon, icon, and apple-icon manifest.json opengraph-image and twitter-image robots.txt sitemap.xml Route Segment Config dynamicParams maxDuration preferredRegion runtime Functions after cacheLife cacheTag unstable_catchError connection cookies draftMode fetch forbidden generateImageMetadata generateMetadata generateSitemaps generateStaticParams generateViewport headers ImageResponse NextRequest NextResponse notFound permanentRedirect redirect refresh revalidatePath revalidateTag unauthorized unstable_cache unstable_noStore unstable_rethrow updateTag useLinkStatus useParams usePathname useReportWebVitals useRouter useSearchParams useSelectedLayoutSegment useSelectedLayoutSegments userAgent Configuration next.config.js adapterPath allowedDevOrigins appDir assetPrefix authInterrupts basePath cacheComponents cacheHandlers cacheLife compress crossOrigin cssChunking deploymentId devIndicators distDir env expireTime exportPathMap generateBuildId generateEtags headers htmlLimitedBots httpAgentOptions images cacheHandler inlineCss logging mdxRs onDemandEntries optimizePackageImports output pageExtensions poweredByHeader productionBrowserSourceMaps proxyClientMaxBodySize reactCompiler reactMaxHeadersLength reactStrictMode redirects rewrites sassOptions serverActions serverComponentsHmrCache serverExternalPackages staleTimes staticGeneration* taint trailingSlash transpilePackages turbopack turbopackFileSystemCache turbopack.ignoreIssue turbopackLocalPostcssConfig typedRoutes typescript urlImports useLightningcss useTypeScriptCli viewTransition webpack webVitalsAttribution TypeScript ESLint CLI create-next-app next CLI Adapters Configuration Creating an Adapter API Reference Testing Adapters Routing with @next/routing Implementing PPR in an Adapter Runtime Integration Invoking Entrypoints Output Types Routing Information Use Cases Edge Runtime Turbopack Glossary Architecture Accessibility Fast Refresh Next.js Compiler Supported Browsers Community Contribution Guide Rspack On this page next/dynamic Examples Importing Client Components Skipping SSR Importing Server Components Loading External Libraries Adding a custom loading component Importing Named Exports Magic Comments webpackIgnore / turbopackIgnore turbopackOptional (Turbopack only) Edit this page on GitHub Scroll to top This page is also available as Markdown at /docs/app/guides/lazy-loading.md . For an index of Next.js documentation , see /docs/llms.txt . App Router Guides Lazy Loading Copy page How to lazy load Client Components and libraries Last updated March 10, 2026 Lazy loading in Next.js helps improve the initial loading performance of an application by decreasing the amount of JavaScript needed to render a route. It allows you to defer loading of Client Components and imported libraries, and only include them in the client bundle when they're needed. For example, you might want to defer loading a modal until a user clicks to open it. There are two ways you can implement lazy loading in Next.js: Using Dynamic Imports with next/dynamic Using React.lazy() with Suspense By default, Server Components are automatically code split , and you can use streaming to progressively send pieces of UI from the server to the client. Lazy loading applies to Client Components. next/dynamic next/dynamic is a composite of React.lazy() and Suspense . It behaves the same way in the app and pages directories to allow for incremental migration. Examples Importing Client Components app/page.js 'use client' import { useState } from 'react' import dynamic from 'next/dynamic' // Client Components: const ComponentA = dynamic (() => import ( '../components/A' )) const ComponentB = dynamic (() => import ( '../components/B' )) const ComponentC = dynamic (() => import ( '../components/C' ) , { ssr : false }) export default function ClientComponentExample () { const [ showMore , setShowMore ] = useState ( false ) return ( < div > { /* Load immediately, but in a separate client bundle */ } < ComponentA /> { /* Load on demand, only when/if the condition is met */ } {showMore && < ComponentB />} < button onClick = {() => setShowMore ( ! showMore)}>Toggle</ button > { /* Load only on the client side */ } < ComponentC /> </ div > ) } Note: When a Server Component dynamically imports a Client Component, automatic code splitting is currently not supported. Skipping SSR When using React.lazy() and Suspense, Client Components will be prerendered (SSR) by default. Note: ssr: false option will only work for Client Components, move it into Client Components ensure the client code-splitting working properly. If you want to disable prerendering for a Client Component, you can use the ssr option set to false : const ComponentC = dynamic (() => import ( '../components/C' ) , { ssr : false }) Importing Server Components If you dynamically import a Server Component, only the Client Components that are children of the Server Component will be lazy-loaded - not the Server Component itself. It will also help preload the static assets such as CSS when you're using it in Server Components. app/page.js import dynamic from 'next/dynamic' // Server Component: const ServerComponent = dynamic (() => import ( '../components/ServerComponent' )) export default function ServerComponentExample () { return ( < div > < ServerComponent /> </ div > ) } Note: ssr: false option is not supported in Server Components. You will see an error if you try to use it in Server Components. ssr: false is not allowed with next/dynamic in Server Components. Please move it into a Client Component. Loading External Libraries External libraries can be loaded on demand using the import() function. This example uses the external library fuse.js for fuzzy search. The module is only loaded on the client after the user types in the search input. app/page.js 'use client' import { useState } from 'react' const names = [ 'Tim' , 'Joe' , 'Bel' , 'Lee' ] export default function Page () { const [ results , setResults ] = useState () return ( < div > < input type = "text" placeholder = "Search" onChange = { async (e) => { const { value } = e .currentTarget // Dynamically load fuse.js const Fuse = ( await import ( 'fuse.js' )).default const fuse = new Fuse (names) setResults ( fuse .search (value)) }} /> < pre >Results: { JSON .stringify (results , null , 2 )}</ pre > </ div > ) } Adding a custom loading component app/page.js 'use client' import dynamic from 'next/dynamic' const WithCustomLoading = dynamic ( () => import ( '../components/WithCustomLoading' ) , { loading : () => < p >Loading...</ p > , } ) export default function Page () { return ( < div > { /* The loading component will be rendered while <WithCustomLoading/> is loading */ } < WithCustomLoading /> </ div > ) } Importing Named Exports To dynamically import a named export, you can return it from the Promise returned by import() function: components/hello.js 'use client' export function Hello () { return < p >Hello!</ p > } app/page.js import dynamic from 'next/dynamic' const ClientComponent = dynamic (() => import ( '../components/hello' ) .then ((mod) => mod .Hello) ) Magic Comments Next.js supports magic comments to control how dynamic imports are handled by the bundler. These comments work with dynamic import() , require() , require.resolve() , and new Worker() expressions. Good to know: Magic comments do not work with static import statements ( import x from 'y' ). They only work with dynamic expressions. webpackIgnore / turbopackIgnore Use these comments to skip bundling a dynamic import. The import expression will be left as-is in the output, useful for runtime-only modules: // Skip bundling - import happens at runtime const runtime = await import ( /* webpackIgnore: true */ 'runtime-module' ) // Turbopack-specific variant const plugin = await import ( /* turbopackIgnore: true */ pluginPath) // Also works with require const mod = require ( /* webpackIgnore: true */ 'runtime-module' ) turbopackOptional (Turbopack only) Use this comment to suppress build errors when a module might not exist. The import will still throw at runtime if the module is missing: // No build error if './optional-feature' doesn't exist // Runtime will throw MODULE_NOT_FOUND if executed const feature = await import ( /* turbopackOptional: true */ './optional-feature' ) // Also works with require const mod = require ( /* turbopackOptional: true */ './optional-module' ) This is useful for: Conditional features that may not be installed Plugin systems where modules are optional Gradual migrations where some files may not exist yet Good to know: webpackOptional is not supported. Use turbopackOptional instead when using Turbopack. Previous JSON-LD Next Development Environment Was this helpful? supported. Send Resources Docs Support Policy Learn Showcase Blog Team Analytics Next.js Conf Previews Evals More Next.js Commerce Contact Sales Community GitHub Releases Telemetry Governance Ecosystem Working Group About Vercel Next.js + Vercel Open Source Software GitHub Bluesky X Legal Privacy Policy Cookie Preferences Subscribe to our newsletter Stay updated on new releases and features, guides, and case studies. Subscribe © 2026 Vercel, Inc.
