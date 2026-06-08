/**
 * Shared path-classification engine for the frontend decomposition ESLint rules
 * (spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md).
 *
 * Lives in lib/ (NOT rules/) so the doc_headline_count_guard ESLint-rule count
 * (which globs eslint-plugin-ax/rules/*.js) is not inflated by a non-rule helper.
 *
 * Project layout (frontend/, `@/*` -> `src/*`):
 *   src/app/        — Next.js routing layer (top)
 *   src/features/<feature>/<slice>/index.ts — feature slices (published via barrel)
 *   src/components/ , src/lib/ — shared kernel (bottom)
 *
 * A "barrel" import targets a directory (resolves to its index) or an explicit
 * index file. A "deep internal" import reaches past a slice barrel into a
 * specific file inside a feature slice.
 */

export const LAYER_RANK = { app: 3, features: 2, shared: 1 };

/** Normalize a path to forward slashes and strip a trailing slash. */
export function norm(p) {
  return String(p).replace(/\\/g, '/').replace(/\/+$/, '');
}

/** Return the `src/...`-relative path for an absolute file, or null if outside src. */
export function toSrcRelative(absFile) {
  const n = norm(absFile);
  const idx = n.lastIndexOf('/src/');
  if (idx >= 0) return 'src/' + n.slice(idx + '/src/'.length);
  if (n.startsWith('src/')) return n;
  return null;
}

/** Resolve a posix-style path with `.`/`..` segments against a base dir. */
export function resolveRelative(baseDir, rel) {
  const out = [];
  for (const seg of (baseDir + '/' + rel).split('/')) {
    if (seg === '' || seg === '.') continue;
    if (seg === '..') out.pop();
    else out.push(seg);
  }
  return out.join('/');
}

/**
 * Resolve an import source to a `src/...`-relative path.
 *  - `@/x`      -> `src/x`
 *  - `./x`/`../x` -> resolved against the importer's dir
 *  - bare specifiers (react, @ax/ui, ...) -> null (out of scope)
 */
export function resolveImport(source, importerSrcRel) {
  if (typeof source !== 'string' || source.length === 0) return null;
  if (source.startsWith('@/')) return 'src/' + source.slice(2);
  if (source.startsWith('./') || source.startsWith('../')) {
    if (!importerSrcRel) return null;
    const dir = importerSrcRel.includes('/')
      ? importerSrcRel.slice(0, importerSrcRel.lastIndexOf('/'))
      : '';
    return resolveRelative(dir, source);
  }
  return null; // bare module specifier
}

/** Classify a `src/...`-relative path into {layer, feature, segsAfterFeature, isBarrel}. */
export function classifySrcPath(srcRel) {
  if (!srcRel) return { layer: null };
  const n = norm(srcRel);
  const parts = n.split('/'); // ['src', ...]
  if (parts[0] !== 'src') return { layer: null };
  const top = parts[1];
  if (top === 'app') return { layer: 'app' };
  if (top === 'components' || top === 'lib') return { layer: 'shared' };
  if (top === 'features') {
    const feature = parts[2] || null;
    const after = parts.slice(3); // segments after `src/features/<feature>`
    const last = after.length ? after[after.length - 1] : '';
    const isIndex = /^index(\.(t|j)sx?)?$/.test(last);
    // barrel: feature root, a single slice dir, or any explicit index file.
    const isBarrel = after.length <= 1 || isIndex;
    return { layer: 'features', feature, segsAfterFeature: after.length, isBarrel };
  }
  return { layer: 'other' };
}

/** layer rank, or 0 for unknown/out-of-tree. */
export function rankOf(layer) {
  return LAYER_RANK[layer] || 0;
}

