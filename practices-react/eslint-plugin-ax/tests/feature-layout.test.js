import test from 'node:test'
import assert from 'node:assert/strict'
import {
  DEFAULT_LAYOUT,
  layoutFrom,
  toSrcRelative,
  resolveImport,
  classifySrcPath,
  isRouteFile,
} from '../lib/feature-layout.js'

// ── layoutFrom fallback contract — every malformed shape must NOT throw and
// must field-by-field fall back to DEFAULT_LAYOUT (D-1 P3: crash-free defaults).
// EXCEPTION: a multi-segment `ax.srcDir` (containing '/') THROWS instead of
// falling back — see the "srcDir must be a single path segment" tests below. ──

test('layoutFrom — undefined settings falls back to DEFAULT_LAYOUT', () => {
  assert.doesNotThrow(() => layoutFrom(undefined))
  assert.deepStrictEqual(layoutFrom(undefined), DEFAULT_LAYOUT)
})

test('layoutFrom — empty settings object falls back to DEFAULT_LAYOUT', () => {
  assert.doesNotThrow(() => layoutFrom({}))
  assert.deepStrictEqual(layoutFrom({}), DEFAULT_LAYOUT)
})

test('layoutFrom — settings.ax === null falls back to DEFAULT_LAYOUT', () => {
  assert.doesNotThrow(() => layoutFrom({ ax: null }))
  assert.deepStrictEqual(layoutFrom({ ax: null }), DEFAULT_LAYOUT)
})

test('layoutFrom — settings.ax as a non-object string falls back to DEFAULT_LAYOUT', () => {
  assert.doesNotThrow(() => layoutFrom({ ax: 'nope' }))
  assert.deepStrictEqual(layoutFrom({ ax: 'nope' }), DEFAULT_LAYOUT)
})

test('layoutFrom — non-string srcDir falls back to default srcDir', () => {
  const layout = layoutFrom({ ax: { srcDir: 42 } })
  assert.equal(layout.srcDir, 'src')
})

test('layoutFrom — empty-string srcDir falls back to default srcDir', () => {
  const layout = layoutFrom({ ax: { srcDir: '' } })
  assert.equal(layout.srcDir, 'src')
})

// ── srcDir must be a single path segment — a multi-segment value is not a
// "malformed field" that degrades gracefully; it silently classifies every
// import in every file as layer:null (see doc comment above layoutFrom), so
// it THROWS instead of falling back to DEFAULT_LAYOUT (P1 fix). ──

test('layoutFrom — multi-segment srcDir throws instead of silently no-oping', () => {
  assert.throws(() => layoutFrom({ ax: { srcDir: 'packages/web/src' } }), /single path segment/)
})

test('layoutFrom — multi-segment srcDir throw message names the offending value', () => {
  assert.throws(() => layoutFrom({ ax: { srcDir: 'packages/web/src' } }), /packages\/web\/src/)
})

test('layoutFrom — leading-slash srcDir also throws (still contains "/")', () => {
  assert.throws(() => layoutFrom({ ax: { srcDir: '/src' } }), /single path segment/)
})

test('layoutFrom — single-segment srcDir does NOT throw and classifies correctly', () => {
  assert.doesNotThrow(() => layoutFrom({ ax: { srcDir: 'source' } }))
  const layout = layoutFrom({ ax: { srcDir: 'source' } })
  assert.equal(layout.srcDir, 'source')
  assert.equal(classifySrcPath('source/features/f1/index.ts', layout).layer, 'features')
})

test('layoutFrom — non-object alias falls back to default alias', () => {
  const layout = layoutFrom({ ax: { alias: 'nope' } })
  assert.deepStrictEqual(layout.alias, { '@/': 'src/' })
})

test('layoutFrom — alias entry with empty key is discarded, not the whole map', () => {
  const layout = layoutFrom({ ax: { alias: { '': 'src/' } } })
  assert.deepStrictEqual(layout.alias, {})
})

test('layoutFrom — alias entry with empty-string value is discarded', () => {
  const layout = layoutFrom({ ax: { alias: { '#/': '' } } })
  assert.deepStrictEqual(layout.alias, {})
})

test('layoutFrom — alias entry with non-string value is discarded', () => {
  const layout = layoutFrom({ ax: { alias: { '#/': 42 } } })
  assert.deepStrictEqual(layout.alias, {})
})

test('layoutFrom — layers.app not an array falls back to default app layer only', () => {
  const layout = layoutFrom({ ax: { layers: { app: 'app' } } })
  assert.deepStrictEqual(layout.layers.app, ['app'])
  assert.deepStrictEqual(layout.layers.features, ['features'])
  assert.deepStrictEqual(layout.layers.shared, ['components', 'lib'])
})

test('layoutFrom — layers.features empty array falls back to default features layer only', () => {
  const layout = layoutFrom({ ax: { layers: { features: [] } } })
  assert.deepStrictEqual(layout.layers.features, ['features'])
})

test('layoutFrom — normal custom layout is honored verbatim', () => {
  const custom = {
    ax: {
      srcDir: 'source',
      alias: { '#/': 'source/' },
      layers: { app: ['pages'], features: ['modules'], shared: ['core', 'ui'] },
    },
  }
  const layout = layoutFrom(custom)
  assert.equal(layout.srcDir, 'source')
  assert.deepStrictEqual(layout.alias, { '#/': 'source/' })
  assert.deepStrictEqual(layout.layers, { app: ['pages'], features: ['modules'], shared: ['core', 'ui'] })
})

test('layoutFrom — field-level fallback: one malformed field does not clobber a well-formed sibling (layers)', () => {
  const layout = layoutFrom({ ax: { srcDir: 'custom', layers: 'nope' } })
  assert.equal(layout.srcDir, 'custom') // preserved
  assert.deepStrictEqual(layout.layers, DEFAULT_LAYOUT.layers) // fell back
})

test('layoutFrom — field-level fallback: one malformed field does not clobber a well-formed sibling (alias)', () => {
  const layout = layoutFrom({ ax: { srcDir: 'custom', alias: { '#/': 42 } } })
  assert.equal(layout.srcDir, 'custom') // preserved
  assert.deepStrictEqual(layout.alias, {}) // bad entry discarded
})

test('layoutFrom — does not mutate the input settings object', () => {
  const settings = Object.freeze({
    ax: Object.freeze({ srcDir: 'source', alias: Object.freeze({ '#/': 'source/' }) }),
  })
  const before = JSON.stringify(settings)
  assert.doesNotThrow(() => layoutFrom(settings))
  assert.equal(JSON.stringify(settings), before)
})

// ── layoutFrom deep-immutability contract (D-track D-1 fix) — the returned
// `layers[layer]` arrays must be fresh copies, not the caller's array by
// reference, and must be frozen at every level so a mutation attempt on the
// result throws instead of silently corrupting the caller's settings. ──

test('layoutFrom — custom layers.app array is copied, not returned by reference', () => {
  const appLayer = ['pages']
  const settings = { ax: { layers: { app: appLayer, features: ['modules'], shared: ['core'] } } }
  const layout = layoutFrom(settings)
  assert.notEqual(layout.layers.app, appLayer)
  assert.deepStrictEqual(layout.layers.app, ['pages'])
})

test('layoutFrom — mutating the input layers.app array after the call does not affect the returned layout', () => {
  const appLayer = ['pages']
  const settings = { ax: { layers: { app: appLayer, features: ['modules'], shared: ['core'] } } }
  const layout = layoutFrom(settings)
  appLayer.push('injected')
  assert.deepStrictEqual(layout.layers.app, ['pages'])
})

test('layoutFrom — returned layers arrays are frozen (custom layout)', () => {
  const settings = { ax: { layers: { app: ['pages'], features: ['modules'], shared: ['core'] } } }
  const layout = layoutFrom(settings)
  assert.equal(Object.isFrozen(layout.layers.app), true)
  assert.equal(Object.isFrozen(layout.layers.features), true)
  assert.equal(Object.isFrozen(layout.layers.shared), true)
  assert.throws(() => layout.layers.app.push('x'), TypeError)
})

test('layoutFrom — pushing onto the returned layers.app array throws and leaves the caller settings array untouched', () => {
  const appLayer = ['pages']
  const settings = { ax: { layers: { app: appLayer, features: ['modules'], shared: ['core'] } } }
  const layout = layoutFrom(settings)
  assert.throws(() => layout.layers.app.push('injected'), TypeError)
  assert.deepStrictEqual(appLayer, ['pages']) // caller's own array is untouched — not shared, not mutated
})

test('layoutFrom — returned layers arrays are frozen (DEFAULT fallback, no settings.ax)', () => {
  const layout = layoutFrom(undefined)
  assert.equal(Object.isFrozen(layout.layers.app), true)
  assert.equal(Object.isFrozen(layout.layers.features), true)
  assert.equal(Object.isFrozen(layout.layers.shared), true)
  assert.throws(() => layout.layers.shared.push('x'), TypeError)
})

test('layoutFrom — DEFAULT fallback layers arrays are not shared by reference with DEFAULT_LAYOUT', () => {
  const layout = layoutFrom(undefined)
  assert.notEqual(layout.layers.app, DEFAULT_LAYOUT.layers.app)
  assert.notEqual(layout.layers.features, DEFAULT_LAYOUT.layers.features)
  assert.notEqual(layout.layers.shared, DEFAULT_LAYOUT.layers.shared)
})

test('layoutFrom — per-field layers fallback (only one layer overridden) still returns fresh frozen arrays', () => {
  const layout = layoutFrom({ ax: { layers: { app: ['pages'] } } })
  assert.notEqual(layout.layers.features, DEFAULT_LAYOUT.layers.features)
  assert.equal(Object.isFrozen(layout.layers.features), true)
  assert.deepStrictEqual(layout.layers.features, ['features'])
})

// ── path functions with a custom layout ────────────────────────────────────

const CUSTOM = layoutFrom({
  ax: {
    srcDir: 'source',
    alias: { '@/': 'source/', '@@/': 'other/' },
    layers: { app: ['pages'], features: ['modules'], shared: ['core', 'ui'] },
  },
})

test('toSrcRelative — honors custom srcDir', () => {
  assert.equal(toSrcRelative('/repo/frontend/source/pages/x/page.tsx', CUSTOM), 'source/pages/x/page.tsx')
  assert.equal(toSrcRelative('/repo/frontend/src/app/page.tsx', CUSTOM), null) // wrong srcDir under custom layout
})

test('resolveImport — longest alias prefix wins', () => {
  assert.equal(resolveImport('@@/x', null, CUSTOM), 'other/x')
  assert.equal(resolveImport('@/x', null, CUSTOM), 'source/x')
})

test('classifySrcPath — classifies custom layer directory names', () => {
  assert.equal(classifySrcPath('source/pages/x/page.tsx', CUSTOM).layer, 'app')
  assert.equal(classifySrcPath('source/core/x.ts', CUSTOM).layer, 'shared')
  assert.equal(classifySrcPath('source/modules/f1/index.ts', CUSTOM).layer, 'features')
})

test('isRouteFile — recognizes routes under a custom app layer directory', () => {
  assert.equal(isRouteFile('/repo/frontend/source/pages/x/page.tsx', CUSTOM), true)
  assert.equal(isRouteFile('/repo/frontend/src/app/x/page.tsx', CUSTOM), false)
})
