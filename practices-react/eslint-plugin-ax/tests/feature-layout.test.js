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
// must field-by-field fall back to DEFAULT_LAYOUT (D-1 P3: crash-free defaults). ──

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
