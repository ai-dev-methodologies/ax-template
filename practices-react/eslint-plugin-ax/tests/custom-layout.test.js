// D-1: proves the 5 layout-aware rules actually thread `settings.ax` through to
// feature-layout.js rather than silently continuing to hardcode `src/app/features/...`.
// A rule that forgets to pass `layout` through would pass its OWN default-layout
// RuleTester suite (tests/<rule>.test.js) while silently failing here — that gap
// is exactly what this file exists to close (PRD D-1 T1-6 / Critic BLOCKER 3).
import { RuleTester } from 'eslint'
import test from 'node:test'
import upwardRule from '../rules/no-upward-layer-import.js'
import crossFeatureRule from '../rules/no-cross-feature-deep-import.js'
import featureInternalRule from '../rules/no-feature-internal-import.js'
import godRouteRule from '../rules/no-god-route.js'
import routeClientDataRule from '../rules/no-route-client-data-fetching.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})
const testerJsx = new RuleTester({
  languageOptions: {
    ecmaVersion: 2024,
    sourceType: 'module',
    parserOptions: { ecmaFeatures: { jsx: true } },
  },
})

// Baseline custom layout (PRD T1-6): all fields non-default.
const CUSTOM_SETTINGS = {
  ax: {
    srcDir: 'source',
    alias: { '#/': 'source/' },
    layers: { app: ['pages'], features: ['modules'], shared: ['core', 'ui'] },
  },
}

const APP = '/repo/frontend/source/pages/x/page.tsx'
const SHARED = '/repo/frontend/source/core/x.ts'
const FEATURE_F1 = '/repo/frontend/source/modules/f1/a.ts'
const FEATURE_F1_ROOT = '/repo/frontend/source/modules/f1/index.ts'

test('ax/no-upward-layer-import — custom layout', () => {
  tester.run('ax/no-upward-layer-import', upwardRule, {
    valid: [
      // downward: app -> features, via barrel — fine
      { code: `import { X } from '#/modules/f1'`, filename: APP, settings: CUSTOM_SETTINGS },
    ],
    invalid: [
      // shared (custom 'core') -> features (custom 'modules') — FORBIDDEN, upward
      {
        code: `import { x } from '#/modules/f1/y'`,
        filename: SHARED,
        settings: CUSTOM_SETTINGS,
        errors: [{ messageId: 'upwardImport' }],
      },
    ],
  })
})

test('ax/no-cross-feature-deep-import — custom layout', () => {
  tester.run('ax/no-cross-feature-deep-import', crossFeatureRule, {
    valid: [
      // cross-feature BARREL import — allowed
      { code: `import { Y } from '#/modules/f2'`, filename: FEATURE_F1, settings: CUSTOM_SETTINGS },
    ],
    invalid: [
      // feature f1 reaching into feature f2 internals — FORBIDDEN
      {
        code: `import { x } from '#/modules/f2/internal/deep'`,
        filename: FEATURE_F1,
        settings: CUSTOM_SETTINGS,
        errors: [{ messageId: 'crossFeatureDeep' }],
      },
    ],
  })
})

test('ax/no-feature-internal-import — custom layout', () => {
  tester.run('ax/no-feature-internal-import', featureInternalRule, {
    valid: [
      // app importing a feature-slice BARREL — the correct public access
      { code: `import { X } from '#/modules/f1'`, filename: APP, settings: CUSTOM_SETTINGS },
    ],
    invalid: [
      // app reaching past the slice barrel into feature internals — FORBIDDEN
      {
        code: `import { LoginForm } from '#/modules/f1/slice/LoginForm'`,
        filename: APP,
        settings: CUSTOM_SETTINGS,
        errors: [{ messageId: 'featureInternal' }],
      },
    ],
  })
})

// build a client route body of N physical lines (mirrors tests/no-god-route.test.js)
function clientRoute(lines) {
  const body = Array.from({ length: lines - 3 }, (_, i) => `  const v${i} = ${i};`).join('\n')
  return `'use client'\nexport default function Page(){\n${body}\n  return null\n}`
}

test('ax/no-god-route — custom layout (custom app layer dir = pages)', () => {
  tester.run('ax/no-god-route', godRouteRule, {
    valid: [
      // short client route under the custom app layer — fine
      { code: `'use client'\nexport default function Page(){ return null }`, filename: APP, settings: CUSTOM_SETTINGS },
      // long file that is NOT under the custom app layer — not a route, not checked
      { code: clientRoute(200), filename: FEATURE_F1_ROOT, settings: CUSTOM_SETTINGS },
    ],
    invalid: [
      // fat client route under the custom app layer — WARN
      {
        code: clientRoute(140),
        filename: APP,
        settings: CUSTOM_SETTINGS,
        errors: [{ messageId: 'godRoute' }],
      },
    ],
  })
})

test('ax/no-route-client-data-fetching — custom layout (custom app layer dir = pages)', () => {
  testerJsx.run('ax/no-route-client-data-fetching', routeClientDataRule, {
    valid: [
      // client route under custom app layer that delegates — fine
      {
        code: `'use client'\nimport { Panel } from '#/modules/f1'\nexport default function Page(){ return <Panel/> }`,
        filename: APP,
        settings: CUSTOM_SETTINGS,
      },
    ],
    invalid: [
      // client route under custom app layer calling raw fetch — FORBIDDEN
      {
        code: `'use client'\nexport default function Page(){ fetch('/api/x'); return null }`,
        filename: APP,
        settings: CUSTOM_SETTINGS,
        errors: [{ messageId: 'clientDataInRoute' }],
      },
    ],
  })
})

// ── Regex-metacharacter cases (R7 — no-god-route / no-route-client-data-fetching
// derive the route-file regex from a layout-supplied app-layer directory name; an
// unescaped '.' or '+' either false-positive-matches or breaks matching). ──────

function dotLayoutSettings() {
  return { ax: { srcDir: 'source', layers: { app: ['pages.v2'] } } }
}
function plusLayoutSettings() {
  return { ax: { srcDir: 'source', layers: { app: ['pages+v2'] } } }
}

test('ax/no-god-route — regex metacharacter escaping in app layer dir name', () => {
  tester.run('ax/no-god-route', godRouteRule, {
    valid: [
      // false-positive control: '.' must NOT act as a wildcard — 'pagesXv2' is not 'pages.v2'
      { code: clientRoute(200), filename: '/repo/frontend/source/pagesXv2/x/page.tsx', settings: dotLayoutSettings() },
      // false-positive control: '+' must NOT act as a quantifier
      { code: clientRoute(200), filename: '/repo/frontend/source/pagesv2/x/page.tsx', settings: plusLayoutSettings() },
      { code: clientRoute(200), filename: '/repo/frontend/source/pagesssv2/x/page.tsx', settings: plusLayoutSettings() },
    ],
    invalid: [
      // positive '.' — literal directory name containing a dot
      {
        code: clientRoute(140),
        filename: '/repo/frontend/source/pages.v2/x/page.tsx',
        settings: dotLayoutSettings(),
        errors: [{ messageId: 'godRoute' }],
      },
      // positive '+' — literal directory name containing a plus
      {
        code: clientRoute(140),
        filename: '/repo/frontend/source/pages+v2/x/page.tsx',
        settings: plusLayoutSettings(),
        errors: [{ messageId: 'godRoute' }],
      },
    ],
  })
})

test('ax/no-route-client-data-fetching — regex metacharacter escaping in app layer dir name', () => {
  testerJsx.run('ax/no-route-client-data-fetching', routeClientDataRule, {
    valid: [
      // false-positive controls — not recognized as a route file, so the client
      // fetch inside is NOT flagged (would be flagged if '.'/'+' escaping were missing
      // in the opposite direction, or if the rule fell through to DEFAULT_LAYOUT).
      {
        code: `'use client'\nexport default function Page(){ fetch('/api/x'); return null }`,
        filename: '/repo/frontend/source/pagesXv2/x/page.tsx',
        settings: dotLayoutSettings(),
      },
      {
        code: `'use client'\nexport default function Page(){ fetch('/api/x'); return null }`,
        filename: '/repo/frontend/source/pagesv2/x/page.tsx',
        settings: plusLayoutSettings(),
      },
      {
        code: `'use client'\nexport default function Page(){ fetch('/api/x'); return null }`,
        filename: '/repo/frontend/source/pagesssv2/x/page.tsx',
        settings: plusLayoutSettings(),
      },
    ],
    invalid: [
      // positive '.' — literal directory name containing a dot
      {
        code: `'use client'\nexport default function Page(){ fetch('/api/x'); return null }`,
        filename: '/repo/frontend/source/pages.v2/x/page.tsx',
        settings: dotLayoutSettings(),
        errors: [{ messageId: 'clientDataInRoute' }],
      },
      // positive '+' — literal directory name containing a plus
      {
        code: `'use client'\nexport default function Page(){ fetch('/api/x'); return null }`,
        filename: '/repo/frontend/source/pages+v2/x/page.tsx',
        settings: plusLayoutSettings(),
        errors: [{ messageId: 'clientDataInRoute' }],
      },
    ],
  })
})

// ── malformed settings.ax must fall back to DEFAULT_LAYOUT, not crash ──────────
// (P3: a broken config must degrade to default enforcement, never to zero enforcement.)

const MALFORMED_SETTINGS = { ax: { srcDir: 42, layers: 'nope' } }
const DEFAULT_SHARED = '/repo/frontend/src/components/nav/Nav.tsx'
const DEFAULT_FEATURE = '/repo/frontend/src/features/f1/a.ts'

test('ax/no-upward-layer-import — malformed settings.ax falls back to DEFAULT_LAYOUT and still detects', () => {
  tester.run('ax/no-upward-layer-import', upwardRule, {
    valid: [],
    invalid: [
      {
        code: `import { x } from '@/features/f1/y'`,
        filename: DEFAULT_SHARED,
        settings: MALFORMED_SETTINGS,
        errors: [{ messageId: 'upwardImport' }],
      },
    ],
  })
})

test('ax/no-cross-feature-deep-import — malformed settings.ax falls back to DEFAULT_LAYOUT and still detects', () => {
  tester.run('ax/no-cross-feature-deep-import', crossFeatureRule, {
    valid: [],
    invalid: [
      {
        code: `import { x } from '@/features/f2/internal/deep'`,
        filename: DEFAULT_FEATURE,
        settings: MALFORMED_SETTINGS,
        errors: [{ messageId: 'crossFeatureDeep' }],
      },
    ],
  })
})
