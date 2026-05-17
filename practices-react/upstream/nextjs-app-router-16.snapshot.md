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
