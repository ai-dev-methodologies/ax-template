/**
 * P1-73 fictional-field LOCK — executable, not manual.
 *
 * The cross-family final gate proved the original lock was prose: the PRD-required
 * repository scans (`grep -rn "verificationState\|providerLinks" templates/ frontend/`,
 * `rg 'roles\[\]'`) were only ever run by hand, so re-introducing the fictional
 * /auth/me shape in the lint/tsc-blind templates/L4 auth page (the fork-receiver
 * deliverable, outside every ESLint/tsc glob per P2-23) passed every AUTOMATED gate.
 * Reviewer's exact reproduction: map `data.roles?.[0]` / `data.verificationState` /
 * `data.providerLinks` back into the page's fetch adapter — view tests, BE golden
 * parity, lint and structural guards all stayed green while the dashboard rendered
 * empty role / unverified / no providers against the real backend.
 *
 * This test IS the scan, wired into `npm run test` and therefore into R25's
 * frontend step. It walks both trees itself; it does not trust anyone to run grep.
 *
 * The fictional tokens are assembled from fragments so this file cannot match its
 * own patterns.
 */
import { describe, expect, it } from 'vitest'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, resolve, sep } from 'node:path'

const FRONTEND_ROOT = resolve(__dirname, '..')
const REPO_ROOT = resolve(FRONTEND_ROOT, '..')
const SCAN_ROOTS = [join(REPO_ROOT, 'templates'), FRONTEND_ROOT]
const SKIP_DIRS = new Set(['node_modules', 'dist', 'build', 'coverage', '.next'])
const SELF = resolve(__filename)

// fragment-assembled so the lock file itself can never trip the lock
const FICTIONAL = [
  'verification' + 'State',
  'provider' + 'Links',
]
// property-ACCESS shapes too, not just the literal type annotation: `data.roles?.[0]`,
// `x.roles[0]`, `.roles.map(...)` — the final gate proved a role-only reintroduction
// evaded the literal-only token. Verified zero legitimate `.roles` uses in either tree.
const ROLES_RE = new RegExp('\\.' + 'roles' + '\\b|' + 'roles' + '\\[' + '\\]')

function* walk(dir: string): Generator<string> {
  for (const name of readdirSync(dir)) {
    const full = join(dir, name)
    const st = statSync(full)
    if (st.isDirectory()) {
      if (!SKIP_DIRS.has(name)) yield* walk(full)
    } else if (/\.(ts|tsx)$/.test(name)) {
      yield full
    }
  }
}

describe('P1-73 canonical /auth/me shape lock (templates/ + frontend/, .ts/.tsx)', () => {
  const hits: string[] = []
  const rolesHits: string[] = []

  for (const root of SCAN_ROOTS) {
    for (const file of walk(root)) {
      if (resolve(file) === SELF) continue
      const text = readFileSync(file, 'utf-8')
      // single regex pass per file (string membership, not array.includes-in-loop)
      const fictionalRe = new RegExp(FICTIONAL.join('|'), 'g')
      for (const m of text.match(fictionalRe) ?? []) {
        hits.push(`${file.split(REPO_ROOT + sep)[1] ?? file}: ${m}`)
      }
      if (ROLES_RE.test(text)) {
        rolesHits.push(file.split(REPO_ROOT + sep)[1] ?? file)
      }
    }
  }

  it('no source file references the fictional verification/provider fields', () => {
    expect(hits, 'fictional /auth/me fields re-introduced — the real backend never ' +
      'sends these; the dashboard would silently render empty (see P1-73)').toEqual([])
  })

  it('no source file references the fictional roles array shape', () => {
    expect(rolesHits, 'the canonical shape has a single `role` string — any roles-' +
      'array access or annotation is the pre-P1-73 fiction').toEqual([])
  })

  it('the scan itself is non-vacuous (it actually visited both trees)', () => {
    // silence-is-not-success: an empty walk would vacuously pass the locks above
    // per-tree floors (the gate showed a single aggregate floor lets one whole
    // tree vanish): current counts are ~401 (templates) and ~432 (frontend)
    for (const root of SCAN_ROOTS) {
      let visited = 0
      for (const _ of walk(root)) visited++
      expect(visited, `scan tree vanished: ${root}`).toBeGreaterThan(100)
    }
  })
})
