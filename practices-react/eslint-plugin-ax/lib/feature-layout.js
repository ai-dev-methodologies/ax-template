/**
 * Shared path-classification engine for the frontend decomposition ESLint rules
 * (spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md).
 *
 * Lives in lib/ (NOT rules/) so the doc_headline_count_guard ESLint-rule count
 * (which globs eslint-plugin-ax/rules/*.js) is not inflated by a non-rule helper.
 *
 * Default project layout (frontend/, `@/*` -> `src/*`):
 *   src/app/        — Next.js routing layer (top)
 *   src/features/<feature>/<slice>/index.ts — feature slices (published via barrel)
 *   src/components/ , src/lib/ — shared kernel (bottom)
 *
 * A fork-receiver with a different layout (custom srcDir / alias / top-level
 * layer directory names) passes it via flat-config `settings.ax` (see
 * `layoutFrom`) — the layer RANKS and the app -> features -> shared direction
 * are NOT customizable (D-track D-1 scope).
 *
 * A "barrel" import targets a directory (resolves to its index) or an explicit
 * index file. A "deep internal" import reaches past a slice barrel into a
 * specific file inside a feature slice.
 */

export const LAYER_RANK = { app: 3, features: 2, shared: 1 }

/** The layout used when a project supplies no `settings.ax` (or an unusable one). */
export const DEFAULT_LAYOUT = Object.freeze({
  srcDir: 'src',
  alias: Object.freeze({ '@/': 'src/' }),
  layers: Object.freeze({
    app: Object.freeze(['app']),
    features: Object.freeze(['features']),
    shared: Object.freeze(['components', 'lib']),
  }),
})

function isPlainObject(v) {
  return v !== null && typeof v === 'object' && !Array.isArray(v)
}

function isNonEmptyStringArray(v) {
  return Array.isArray(v) && v.length > 0 && v.every((s) => typeof s === 'string' && s.length > 0)
}

/** Escape a literal string for safe embedding inside a `new RegExp(...)` source. */
function escapeRe(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * Derive a layout from ESLint flat-config `context.settings`. Never throws —
 * any missing/malformed field falls back to its DEFAULT_LAYOUT counterpart,
 * field by field, so a partially-broken config still enforces on the fields
 * that ARE well-formed instead of degrading enforcement entirely.
 *
 * - `settings` not an object, or `settings.ax` not an object -> DEFAULT_LAYOUT.
 * - `ax.srcDir` not a non-empty string -> default `'src'`.
 * - `ax.alias` not a plain object -> default `{'@/':'src/'}`; if it IS an
 *   object, each entry is validated independently — a non-string/empty key or
 *   non-string/empty value discards only that entry (not the whole map).
 * - `ax.layers` not a plain object -> default layers entirely; if it IS an
 *   object, each of `app`/`features`/`shared` is validated independently — a
 *   value that isn't a non-empty array of non-empty strings falls back to
 *   that one layer's default.
 *
 * The returned object is frozen and never aliases (nor mutates) `settings`.
 */
export function layoutFrom(settings) {
  const ax = isPlainObject(settings) ? settings.ax : undefined
  if (!isPlainObject(ax)) return DEFAULT_LAYOUT

  const srcDir = typeof ax.srcDir === 'string' && ax.srcDir.length > 0 ? ax.srcDir : DEFAULT_LAYOUT.srcDir

  let alias = DEFAULT_LAYOUT.alias
  if (isPlainObject(ax.alias)) {
    const filtered = {}
    for (const [k, v] of Object.entries(ax.alias)) {
      if (typeof k === 'string' && k.length > 0 && typeof v === 'string' && v.length > 0) {
        filtered[k] = v
      }
    }
    alias = filtered
  }

  let layers = DEFAULT_LAYOUT.layers
  if (isPlainObject(ax.layers)) {
    layers = {
      app: isNonEmptyStringArray(ax.layers.app) ? ax.layers.app : DEFAULT_LAYOUT.layers.app,
      features: isNonEmptyStringArray(ax.layers.features) ? ax.layers.features : DEFAULT_LAYOUT.layers.features,
      shared: isNonEmptyStringArray(ax.layers.shared) ? ax.layers.shared : DEFAULT_LAYOUT.layers.shared,
    }
  }

  return Object.freeze({ srcDir, alias: Object.freeze(alias), layers: Object.freeze(layers) })
}

/** Normalize a path to forward slashes and strip a trailing slash. */
export function norm(p) {
  return String(p).replace(/\\/g, '/').replace(/\/+$/, '')
}

/** Return the `<srcDir>/...`-relative path for an absolute file, or null if outside srcDir. */
export function toSrcRelative(absFile, layout = DEFAULT_LAYOUT) {
  const n = norm(absFile)
  const marker = `/${layout.srcDir}/`
  const idx = n.lastIndexOf(marker)
  if (idx >= 0) return `${layout.srcDir}/` + n.slice(idx + marker.length)
  if (n.startsWith(`${layout.srcDir}/`)) return n
  return null
}

/** Resolve a posix-style path with `.`/`..` segments against a base dir. */
export function resolveRelative(baseDir, rel) {
  const out = []
  for (const seg of (baseDir + '/' + rel).split('/')) {
    if (seg === '' || seg === '.') continue
    if (seg === '..') out.pop()
    else out.push(seg)
  }
  return out.join('/')
}

/**
 * Resolve an import source to a `<srcDir>/...`-relative path.
 *  - an alias prefix (e.g. `@/x`) -> `<replacement>x`, longest prefix wins
 *    when multiple aliases share a prefix (e.g. `@/` and `@@/`).
 *  - `./x`/`../x` -> resolved against the importer's dir
 *  - bare specifiers (react, @ax/ui, ...) -> null (out of scope)
 */
export function resolveImport(source, importerSrcRel, layout = DEFAULT_LAYOUT) {
  if (typeof source !== 'string' || source.length === 0) return null
  const aliasEntries = Object.entries(layout.alias).sort((a, b) => b[0].length - a[0].length)
  for (const [prefix, replacement] of aliasEntries) {
    if (source.startsWith(prefix)) return replacement + source.slice(prefix.length)
  }
  if (source.startsWith('./') || source.startsWith('../')) {
    if (!importerSrcRel) return null
    const dir = importerSrcRel.includes('/')
      ? importerSrcRel.slice(0, importerSrcRel.lastIndexOf('/'))
      : ''
    return resolveRelative(dir, source)
  }
  return null // bare module specifier
}

/** Classify a `<srcDir>/...`-relative path into {layer, feature, segsAfterFeature, isBarrel}. */
export function classifySrcPath(srcRel, layout = DEFAULT_LAYOUT) {
  if (!srcRel) return { layer: null }
  const n = norm(srcRel)
  const parts = n.split('/') // [<srcDir>, ...]
  if (parts[0] !== layout.srcDir) return { layer: null }
  const top = parts[1]
  if (layout.layers.app.includes(top)) return { layer: 'app' }
  if (layout.layers.shared.includes(top)) return { layer: 'shared' }
  if (layout.layers.features.includes(top)) {
    const feature = parts[2] || null
    const after = parts.slice(3) // segments after `<srcDir>/<featuresDir>/<feature>`
    const last = after.length ? after[after.length - 1] : ''
    // index barrel: index(.ts|tsx|js|jsx|mjs|cjs|mts|cts) — all module extensions.
    const isIndex = /^index(\.[mc]?[tj]sx?)?$/.test(last)
    // barrel: feature root, a single slice dir, or any explicit index file.
    const isBarrel = after.length <= 1 || isIndex
    return { layer: 'features', feature, segsAfterFeature: after.length, isBarrel }
  }
  return { layer: 'other' }
}

/** layer rank, or 0 for unknown/out-of-tree. */
export function rankOf(layer) {
  return LAYER_RANK[layer] || 0
}

/**
 * Build an ESLint visitor that fires `check(sourceValue, reportNode)` for EVERY import
 * form — static `import ... from 'x'`, dynamic `import('x')`, and `require('x')` — so a
 * rule cannot be bypassed by switching import syntax (audit 2026-06-08 HIGH finding).
 */
export function importVisitors(check) {
  function fromLiteral(arg) {
    return arg && arg.type === 'Literal' && typeof arg.value === 'string' ? arg.value : null
  }
  return {
    ImportDeclaration(node) {
      check(node.source && node.source.value, node)
    },
    // dynamic import('x')
    ImportExpression(node) {
      const v = fromLiteral(node.source)
      if (v != null) check(v, node)
    },
    // require('x')
    'CallExpression[callee.name="require"]'(node) {
      const v = fromLiteral(node.arguments && node.arguments[0])
      if (v != null) check(v, node)
    },
  }
}

/** Is this a Next.js App Router route file (`<srcDir>/<appDir>/**\/page|layout`)? */
export function isRouteFile(filename, layout = DEFAULT_LAYOUT) {
  const n = norm(filename)
  const srcDir = escapeRe(layout.srcDir)
  return layout.layers.app.some((appDir) => {
    const a = escapeRe(appDir)
    return (
      new RegExp(`(^|/)${srcDir}/${a}/.*/(page|layout)\\.(t|j)sx?$`).test(n) ||
      new RegExp(`(^|/)${srcDir}/${a}/(page|layout)\\.(t|j)sx?$`).test(n)
    )
  })
}

/** Does the program start with a `"use client"` directive? */
export function hasUseClientDirective(programNode) {
  const body = programNode && programNode.body
  if (!Array.isArray(body)) return false
  for (const stmt of body) {
    if (
      stmt.type === 'ExpressionStatement' &&
      stmt.expression &&
      stmt.expression.type === 'Literal' &&
      stmt.expression.value === 'use client'
    ) {
      return true
    }
    // directives are only at the very top; stop at the first non-directive
    if (stmt.type !== 'ExpressionStatement' || typeof (stmt.expression && stmt.expression.value) !== 'string') {
      break
    }
  }
  return false
}
