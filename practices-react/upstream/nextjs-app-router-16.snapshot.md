# nextjs-app-router-16 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://nextjs.org/docs/app/getting-started/installation (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:55Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://nextjs.org/docs/app/getting-started/installation`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r091`
**Body SHA-256 (below the `---` divider, header excluded):** 7d63149c1805b05d5c314995542872079798fb6036aee2efafb92753e75a8e23

---

---
snapshot_id: nextjs-app-router-16
source: "https://nextjs.org/docs/app/getting-started/installation"
fetched_at: "2026-05-17T13:00:00Z"
version_observed: "next@16.2.6"
via: WebFetch
sha: "a3f1b2c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2"
---

# Next.js 16 App Router — Installation & Getting Started

Source: https://nextjs.org/docs/app/getting-started/installation  
Version: next@16.2.6 · lastUpdated: 2026-05-13

## Quick Start

```bash
pnpm create next-app@latest my-app --yes
cd my-app
pnpm dev
```

The `--yes` flag enables TypeScript, Tailwind CSS, ESLint, App Router, and Turbopack by default.

## System Requirements

- Minimum Node.js version: 20.9
- Turbopack is the default bundler (`next dev` uses Turbopack). Use `--webpack` to opt out.

## App Directory Structure

Next.js uses file-system routing. The `app/` directory is the root for App Router pages.

```
app/
├── layout.tsx   # root layout (required, must contain <html> and <body>)
└── page.tsx     # home page route /
```

```tsx
// app/layout.tsx
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
```

## TypeScript Support

Next.js includes a custom TypeScript plugin and type checker. Minimum TypeScript version: v5.1.0.

Enable in VS Code:
1. Open command palette (Ctrl/⌘ + Shift + P)
2. Search "TypeScript: Select TypeScript Version"
3. Select "Use Workspace Version"

## Linting

Starting with Next.js 16, `next build` no longer runs the linter automatically.
Use npm scripts to run ESLint separately.

## Module Path Aliases

```json
// tsconfig.json
{
  "compilerOptions": {
    "baseUrl": "src/",
    "paths": { "@/components/*": ["components/*"] }
  }
}
```

## Key Scripts

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "eslint"
  }
}
```

## Supported Browsers

Chrome 111+, Edge 111+, Firefox 111+, Safari 16.4+

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://nextjs.org/docs/app/getting-started/installation
HTTP status: 200 · extracted bytes: 14611 · sha256: 0b7877a0fd8c861a7c27a113802a598ec50b569630f051e2daac5bb7dd873422
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r091`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Getting Started: Installation | Next.js Skip to content Search documentation... ⌘K Search... ⌘K Showcase Docs Blog Templates Enterprise Search documentation... ⌘K Search... ⌘K Feedback Learn Menu Using App Router Features available in /app Latest Version 16.2.12 Getting Started Installation Project Structure Layouts and Pages Linking and Navigating Server and Client Components Fetching Data Mutating Data Caching Revalidating Error Handling CSS Image Optimization Font Optimization Metadata and OG images Route Handlers Proxy Deploying Upgrading Guides AI Coding Agents Analytics Authentication Backend for Frontend Caching (Previous Model) CDN Caching CI Build Caching Content Security Policy CSS-in-JS Custom Server Data Security Debugging Deploying to Platforms Draft Mode Environment Variables Forms How Revalidation Works ISR Instrumentation Internationalization JSON-LD Lazy Loading Development Environment Next.js MCP Server MDX Memory Usage Migrating App Router Create React App Vite Migrating to Cache Components Multi-tenant Multi-zones OpenTelemetry Package Bundling PPR Platform Guide Prefetching Preserving UI state Preventing Flash Production PWAs Public pages Redirecting Rendering Philosophy Sass Scripts Self-Hosting Server Actions SPAs Static Exports Streaming Tailwind CSS v3 Testing Cypress Jest Playwright Vitest Third Party Libraries Upgrading Codemods Version 14 Version 15 Version 16 Videos View transitions API Reference Directives use cache use cache: private use cache: remote use client use server Components Font Form Component Image Component Link Component Script Component File-system conventions default.js Dynamic Segments error.js forbidden.js instrumentation.js instrumentation-client.js Intercepting Routes layout.js loading.js mdx-components.js not-found.js page.js Parallel Routes proxy.js public route.js Route Groups src template.js unauthorized.js Metadata Files favicon, icon, and apple-icon manifest.json opengraph-image and twitter-image robots.txt sitemap.xml Route Segment Config dynamicParams maxDuration preferredRegion runtime Functions after cacheLife cacheTag unstable_catchError connection cookies draftMode fetch forbidden generateImageMetadata generateMetadata generateSitemaps generateStaticParams generateViewport headers ImageResponse NextRequest NextResponse notFound permanentRedirect redirect refresh revalidatePath revalidateTag unauthorized unstable_cache unstable_noStore unstable_rethrow updateTag useLinkStatus useParams usePathname useReportWebVitals useRouter useSearchParams useSelectedLayoutSegment useSelectedLayoutSegments userAgent Configuration next.config.js adapterPath allowedDevOrigins appDir assetPrefix authInterrupts basePath cacheComponents cacheHandlers cacheLife compress crossOrigin cssChunking deploymentId devIndicators distDir env expireTime exportPathMap generateBuildId generateEtags headers htmlLimitedBots httpAgentOptions images cacheHandler inlineCss logging mdxRs onDemandEntries optimizePackageImports output pageExtensions poweredByHeader productionBrowserSourceMaps proxyClientMaxBodySize reactCompiler reactMaxHeadersLength reactStrictMode redirects rewrites sassOptions serverActions serverComponentsHmrCache serverExternalPackages staleTimes staticGeneration* taint trailingSlash transpilePackages turbopack turbopackFileSystemCache turbopack.ignoreIssue turbopackLocalPostcssConfig typedRoutes typescript urlImports useLightningcss useTypeScriptCli viewTransition webpack webVitalsAttribution TypeScript ESLint CLI create-next-app next CLI Adapters Configuration Creating an Adapter API Reference Testing Adapters Routing with @next/routing Implementing PPR in an Adapter Runtime Integration Invoking Entrypoints Output Types Routing Information Use Cases Edge Runtime Turbopack Glossary Architecture Accessibility Fast Refresh Next.js Compiler Supported Browsers Community Contribution Guide Rspack On this page Quick start System requirements Supported browsers Create with the CLI Manual installation Create the app directory Create the public folder (optional) Run the development server Set up TypeScript IDE Plugin Set up your editor Set up linting Set up Absolute Imports and Module Path Aliases Edit this page on GitHub Scroll to top This page is also available as Markdown at /docs/app/getting-started/installation.md . For an index of Next.js documentation , see /docs/llms.txt . App Router Getting Started Installation Copy page Installation Last updated July 22, 2026 Create a new Next.js app and run it locally. Quick start Create a new Next.js app named my-app cd my-app and start the dev server. Visit http://localhost:3000 . pnpm npm yarn bun Terminal pnpm create next-app@latest my-app --yes cd my-app pnpm dev --yes skips prompts using saved preferences or defaults. The default setup enables TypeScript, Tailwind CSS, ESLint, App Router, and Turbopack, with import alias @/* , and includes AGENTS.md (with a CLAUDE.md that references it) to guide coding agents to write up-to-date Next.js code. System requirements Before you begin, make sure your development environment meets the following requirements: Minimum Node.js version: 20.9 Operating systems: macOS, Windows (including WSL), and Linux. Supported browsers Next.js supports modern browsers with zero configuration. Chrome 111+ Edge 111+ Firefox 111+ Safari 16.4+ Learn more about browser support , including how to configure polyfills and target specific browsers. Create with the CLI The quickest way to create a new Next.js app is using create-next-app , which sets up everything automatically for you. To create a project, run: pnpm npm yarn bun Terminal pnpm create next-app On installation, you'll see the following prompts: Terminal What is your project named? my-app Would you like to use the recommended Next.js defaults? Yes, use recommended defaults - TypeScript, ESLint, Tailwind CSS, App Router, AGENTS.md No, reuse previous settings No, customize settings - Choose your own preferences If you choose to customize settings , you'll see the following prompts: Terminal Would you like to use TypeScript? No / Yes Which linter would you like to use? ESLint / Biome / None Would you like to use React Compiler? No / Yes Would you like to use Tailwind CSS? No / Yes Would you like your code inside a `src/` directory? No / Yes Would you like to use App Router? (recommended) No / Yes Would you like to customize the import alias (`@/*` by default)? No / Yes What import alias would you like configured? @/* Would you like to include AGENTS.md to guide coding agents to write up-to-date Next.js code? No / Yes After the prompts, create-next-app will create a folder with your project name and install the required dependencies. Manual installation To manually create a new Next.js app, install the required packages: pnpm npm yarn bun Terminal pnpm i next@latest react@latest react-dom@latest Good to know : The App Router uses React canary releases built-in, which include all the stable React 19 changes, as well as newer features being validated in frameworks, but you should still declare react and react-dom in package.json for tooling and ecosystem compatibility. The Pages Router uses the React version from your package.json . Then, add the following scripts to your package.json file: package.json { "scripts" : { "dev" : "next dev" , "build" : "next build" , "start" : "next start" , "lint" : "eslint" , "lint:fix" : "eslint --fix" } } These scripts refer to the different stages of developing an application: next dev : Starts the development server using Turbopack (default bundler). next build : Builds the application for production. next start : Starts the production server. eslint : Runs ESLint. Turbopack is now the default bundler. To use Webpack run next dev --webpack or next build --webpack . See the Turbopack docs for configuration details. Create the app directory Next.js uses file-system routing, which means the routes in your application are determined by how you structure your files. Create an app folder. Then, inside app , create a layout.tsx file. This file is the root layout . It's required and must contain the <html> and <body> tags. app/layout.tsx TypeScript JavaScript TypeScript export default function RootLayout ({ children , } : { children : React . ReactNode }) { return ( < html lang = "en" > < body >{children}</ body > </ html > ) } Create a home page app/page.tsx with some initial content: app/page.tsx TypeScript JavaScript TypeScript export default function Page () { return < h1 >Hello, Next.js!</ h1 > } Both layout.tsx and page.tsx will be rendered when the user visits the root of your application ( / ). Good to know : If you forget to create the root layout, Next.js will automatically create this file when running the development server with next dev . You can optionally use a src folder in the root of your project to separate your application's code from configuration files. Create the public folder (optional) Create a public folder at the root of your project to store static assets such as images, fonts, etc. Files inside public can then be referenced by your code starting from the base URL ( / ). You can then reference these assets using the root path ( / ). For example, public/profile.png can be referenced as /profile.png : app/page.tsx TypeScript JavaScript TypeScript import Image from 'next/image' export default function Page () { return < Image src = "/profile.png" alt = "Profile" width = { 100 } height = { 100 } /> } Run the development server Run npm run dev to start the development server. Visit http://localhost:3000 to view your application. Edit the app/page.tsx file and save it to see the updated result in your browser. Set up TypeScript Minimum TypeScript version: v5.1.0 Next.js comes with built-in TypeScript support. To add TypeScript to your project, rename a file to .ts / .tsx and run next dev . Next.js will automatically install the necessary dependencies and add a tsconfig.json file with the recommended config options. IDE Plugin Next.js includes a custom TypeScript plugin and type checker, which VSCode and other code editors can use for advanced type-checking and auto-completion. You can enable the plugin in VS Code by: Opening the command palette ( Ctrl/⌘ + Shift + P ) Searching for "TypeScript: Select TypeScript Version" Selecting "Use Workspace Version" See the TypeScript reference page for more information. Set up your editor The App Router names files by convention, like page.tsx , layout.tsx , and route.ts , so your editor quickly fills with same-named tabs. Label each tab with its enclosing folders, like blog/[id] , so you can tell them apart. In VS Code 1.88+ or Cursor, add custom editor labels to .vscode/settings.json . Labeling two folders deep keeps dynamic routes like blog/[id]/page.tsx from all collapsing to the same [id] label: .vscode/settings.json { "workbench.editor.customLabels.patterns" : { "**/app/**/page.tsx" : "${dirname(1)}/${dirname} - page.tsx" , "**/app/**/layout.tsx" : "${dirname(1)}/${dirname} - layout.tsx" , "**/app/**/loading.tsx" : "${dirname(1)}/${dirname} - loading.tsx" , "**/app/**/error.tsx" : "${dirname(1)}/${dirname} - error.tsx" , "**/app/**/not-found.tsx" : "${dirname(1)}/${dirname} - not-found.tsx" , "**/app/**/template.tsx" : "${dirname(1)}/${dirname} - template.tsx" , "**/app/**/default.tsx" : "${dirname(1)}/${dirname} - default.tsx" , "**/app/**/route.ts" : "${dirname(1)}/${dirname} - route.ts" } } Or copy this prompt to have your coding agent set it up: Prompt Copy prompt Set up custom editor labels so my Next.js App Router files are easy to tell apart. Read https://nextjs.org/docs/app/getting-started/installation#set-up-your-editor and add the workbench.editor.customLabels.patterns config shown there to my .vscode/settings.json, creating the file if it doesn't exist. Adjust the labels to taste. If I use a different editor, apply the equivalent setting or tell me it's automatic, and leave my other settings untouched. Good to know: JetBrains IDEs (WebStorm, IntelliJ) show the folder for same-named files automatically, so no setup is needed. Set up linting Next.js supports linting with either ESLint or Biome. Choose a linter and run it directly via package.json scripts. Use ESLint (comprehensive rules): package.json { "scripts" : { "lint" : "eslint" , "lint:fix" : "eslint --fix" } } Or use Biome (fast linter + formatter): package.json { "scripts" : { "lint" : "biome check" , "format" : "biome format --write" } } If your project previously used next lint , migrate your scripts to the ESLint CLI with the codemod: Terminal npx @next/codemod@canary next-lint-to-eslint-cli . If you use ESLint, create an explicit config (recommended eslint.config.mjs ). ESLint supports both the legacy .eslintrc.* and the newer eslint.config.mjs formats . See the ESLint API reference for a recommended setup. Good to know : Starting with Next.js 16, next build no longer runs the linter automatically. Instead, you can run your linter through NPM scripts. See the ESLint Plugin page for more information. Set up Absolute Imports and Module Path Aliases Next.js has in-built support for the "paths" and "baseUrl" options of tsconfig.json and jsconfig.json files. These options allow you to alias project directories to absolute paths, making it easier and cleaner to import modules. For example: // Before import { Button } from '../../../components/button' // After import { Button } from '@/components/button' To configure absolute imports, add the baseUrl configuration option to your tsconfig.json or jsconfig.json file. For example: tsconfig.json or jsconfig.json { "compilerOptions" : { "baseUrl" : "src/" } } In addition to configuring the baseUrl path, you can use the "paths" option to "alias" module paths. For example, the following configuration maps @/components/* to components/* : tsconfig.json or jsconfig.json { "compilerOptions" : { "baseUrl" : "src/" , "paths" : { "@/styles/*" : [ "styles/*" ] , "@/components/*" : [ "components/*" ] } } } Each of the "paths" are relative to the baseUrl location. Previous Getting Started Next Project Structure Was this helpful? supported. Send Resources Docs Support Policy Learn Showcase Blog Team Analytics Next.js Conf Previews Evals More Next.js Commerce Contact Sales Community GitHub Releases Telemetry Governance Ecosystem Working Group About Vercel Next.js + Vercel Open Source Software GitHub Bluesky X Legal Privacy Policy Cookie Preferences Subscribe to our newsletter Stay updated on new releases and features, guides, and case studies. Subscribe © 2026 Vercel, Inc.
