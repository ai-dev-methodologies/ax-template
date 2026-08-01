# nextjs-optimize-package-imports — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:54Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r090`
**Body SHA-256 (below the `---` divider, header excluded):** 9bf775312d005e7d1edbfd14970d925b3d54998799bedcf07a2f19ec82c10aec

---

# Snapshot: Next.js 16 — experimental.optimizePackageImports

- **source**: https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13 (per source frontmatter)
- **version_observed**: 16.2.6
- **via**: WebFetch

## Status notice (verbatim)

> "This feature is currently experimental and subject to change, it's not recommended for production. Try it out and share your feedback on GitHub."

## What it does (verbatim)

> "Some packages can export hundreds or thousands of modules, which can cause performance issues in development and production."

> "Adding a package to `experimental.optimizePackageImports` will only load the modules you are actually using, while still giving you the convenience of writing import statements with many named exports."

## Configuration example (verbatim)

```js
// next.config.js
module.exports = {
  experimental: {
    optimizePackageImports: ['package-name'],
  },
}
```

## Default-optimized package list (verbatim, as of 16.2.6)

The following libraries are optimized by default and do **not** need to be added:

- `lucide-react`
- `date-fns`
- `lodash-es`
- `ramda`
- `antd`
- `react-bootstrap`
- `ahooks`
- `@ant-design/icons`
- `@headlessui/react`
- `@headlessui-float/react`
- `@heroicons/react/20/solid`
- `@heroicons/react/24/solid`
- `@heroicons/react/24/outline`
- `@visx/visx`
- `@tremor/react`
- `rxjs`
- `@mui/material`
- `@mui/icons-material`
- `recharts`
- `react-use`
- `@material-ui/core`
- `@material-ui/icons`
- `@tabler/icons-react`
- `mui-core`
- `react-icons/*`
- `effect`
- `@effect/*`

## Audit implication

- Many libraries the Vercel react-best-practices `bundle-barrel-imports` rule lists as "affected" are already optimized by default in Next.js 16. The rule's "add to optimizePackageImports" instruction would be a **no-op** for these.
- The feature being experimental in a current stable Next release means production-grade catalogs cannot present it as the canonical fix without a caveat.

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://nextjs.org/docs/app/api-reference/config/next-config-js/optimizePackageImports
HTTP status: 200 · extracted bytes: 5773 · sha256: a6c27842c1a07ef4617b017f9814207484527d70f52869c0873c39b53958da37
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r090`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

next.config.js: optimizePackageImports | Next.js Skip to content Search documentation... ⌘K Search... ⌘K Showcase Docs Blog Templates Enterprise Search documentation... ⌘K Search... ⌘K Feedback Learn Menu Using App Router Features available in /app Latest Version 16.2.12 Getting Started Installation Project Structure Layouts and Pages Linking and Navigating Server and Client Components Fetching Data Mutating Data Caching Revalidating Error Handling CSS Image Optimization Font Optimization Metadata and OG images Route Handlers Proxy Deploying Upgrading Guides AI Coding Agents Analytics Authentication Backend for Frontend Caching (Previous Model) CDN Caching CI Build Caching Content Security Policy CSS-in-JS Custom Server Data Security Debugging Deploying to Platforms Draft Mode Environment Variables Forms How Revalidation Works ISR Instrumentation Internationalization JSON-LD Lazy Loading Development Environment Next.js MCP Server MDX Memory Usage Migrating App Router Create React App Vite Migrating to Cache Components Multi-tenant Multi-zones OpenTelemetry Package Bundling PPR Platform Guide Prefetching Preserving UI state Preventing Flash Production PWAs Public pages Redirecting Rendering Philosophy Sass Scripts Self-Hosting Server Actions SPAs Static Exports Streaming Tailwind CSS v3 Testing Cypress Jest Playwright Vitest Third Party Libraries Upgrading Codemods Version 14 Version 15 Version 16 Videos View transitions API Reference Directives use cache use cache: private use cache: remote use client use server Components Font Form Component Image Component Link Component Script Component File-system conventions default.js Dynamic Segments error.js forbidden.js instrumentation.js instrumentation-client.js Intercepting Routes layout.js loading.js mdx-components.js not-found.js page.js Parallel Routes proxy.js public route.js Route Groups src template.js unauthorized.js Metadata Files favicon, icon, and apple-icon manifest.json opengraph-image and twitter-image robots.txt sitemap.xml Route Segment Config dynamicParams maxDuration preferredRegion runtime Functions after cacheLife cacheTag unstable_catchError connection cookies draftMode fetch forbidden generateImageMetadata generateMetadata generateSitemaps generateStaticParams generateViewport headers ImageResponse NextRequest NextResponse notFound permanentRedirect redirect refresh revalidatePath revalidateTag unauthorized unstable_cache unstable_noStore unstable_rethrow updateTag useLinkStatus useParams usePathname useReportWebVitals useRouter useSearchParams useSelectedLayoutSegment useSelectedLayoutSegments userAgent Configuration next.config.js adapterPath allowedDevOrigins appDir assetPrefix authInterrupts basePath cacheComponents cacheHandlers cacheLife compress crossOrigin cssChunking deploymentId devIndicators distDir env expireTime exportPathMap generateBuildId generateEtags headers htmlLimitedBots httpAgentOptions images cacheHandler inlineCss logging mdxRs onDemandEntries optimizePackageImports output pageExtensions poweredByHeader productionBrowserSourceMaps proxyClientMaxBodySize reactCompiler reactMaxHeadersLength reactStrictMode redirects rewrites sassOptions serverActions serverComponentsHmrCache serverExternalPackages staleTimes staticGeneration* taint trailingSlash transpilePackages turbopack turbopackFileSystemCache turbopack.ignoreIssue turbopackLocalPostcssConfig typedRoutes typescript urlImports useLightningcss useTypeScriptCli viewTransition webpack webVitalsAttribution TypeScript ESLint CLI create-next-app next CLI Adapters Configuration Creating an Adapter API Reference Testing Adapters Routing with @next/routing Implementing PPR in an Adapter Runtime Integration Invoking Entrypoints Output Types Routing Information Use Cases Edge Runtime Turbopack Glossary Architecture Accessibility Fast Refresh Next.js Compiler Supported Browsers Community Contribution Guide Rspack Edit this page on GitHub Scroll to top This page is also available as Markdown at /docs/app/api-reference/config/next-config-js/optimizePackageImports.md . For an index of Next.js documentation , see /docs/llms.txt . Configuration next.config.js optimizePackageImports Copy page optimizePackageImports This feature is currently experimental and subject to change, it's not recommended for production. Try it out and share your feedback on GitHub . Last updated December 19, 2025 Some packages can export hundreds or thousands of modules, which can cause performance issues in development and production. Adding a package to experimental.optimizePackageImports will only load the modules you are actually using, while still giving you the convenience of writing import statements with many named exports. next.config.js module . exports = { experimental : { optimizePackageImports : [ 'package-name' ] , } , } The following libraries are optimized by default: lucide-react date-fns lodash-es ramda antd react-bootstrap ahooks @ant-design/icons @headlessui/react @headlessui-float/react @heroicons/react/20/solid @heroicons/react/24/solid @heroicons/react/24/outline @visx/visx @tremor/react rxjs @mui/material @mui/icons-material recharts react-use @material-ui/core @material-ui/icons @tabler/icons-react mui-core react-icons/* effect @effect/* Previous onDemandEntries Next output Was this helpful? supported. Send Resources Docs Support Policy Learn Showcase Blog Team Analytics Next.js Conf Previews Evals More Next.js Commerce Contact Sales Community GitHub Releases Telemetry Governance Ecosystem Working Group About Vercel Next.js + Vercel Open Source Software GitHub Bluesky X Legal Privacy Policy Cookie Preferences Subscribe to our newsletter Stay updated on new releases and features, guides, and case studies. Subscribe © 2026 Vercel, Inc.
