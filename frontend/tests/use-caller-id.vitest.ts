import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// use-caller-id.ts (L0 fork-receiver-kit) was UNTESTED before this file — the
// production hard-stop (R47 rbac-stub-default-fail-closed: throw in prod rather
// than silently return a fake caller) and the dev fail-closed default (default to
// 'user', not 'admin') are exactly the invariants ax/no-caller-identity-from-props
// tells callers to rely on. Untested, a regression here (e.g. the prod throw
// silently downgraded to a warn-and-continue) would defeat the whole seam.
//
// The hooks memoize a "warned once" flag at MODULE scope, so each test that cares
// about a specific env/warn combination re-imports the module fresh via
// vi.resetModules() + dynamic import, matching the module's own documented
// module-scoped-flag design instead of fighting it.

// Vite can only statically rewrite a dynamic import() with a LITERAL specifier,
// so the path is inlined at every call site rather than passed through a variable.
async function freshImport() {
  vi.resetModules()
  return import('../../templates/L0/fork-receiver-kit/use-caller-id')
}

const ORIGINAL_NODE_ENV = process.env.NODE_ENV
const ORIGINAL_DEV_AS_ADMIN = process.env.NEXT_PUBLIC_DEV_AS_ADMIN

describe('useCallerId / useCallerRole — production hard-stop + dev fail-closed default', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  afterEach(() => {
    process.env.NODE_ENV = ORIGINAL_NODE_ENV
    if (ORIGINAL_DEV_AS_ADMIN === undefined) delete process.env.NEXT_PUBLIC_DEV_AS_ADMIN
    else process.env.NEXT_PUBLIC_DEV_AS_ADMIN = ORIGINAL_DEV_AS_ADMIN
  })

  it('useCallerId returns the demo-user stub in a non-production env', async () => {
    process.env.NODE_ENV = 'test'
    const { useCallerId } = await freshImport()
    expect(useCallerId()).toBe('demo-user')
  })

  it('useCallerId THROWS in production rather than returning a fake identity', async () => {
    process.env.NODE_ENV = 'production'
    const { useCallerId } = await freshImport()
    expect(() => useCallerId()).toThrow(/Identity provider not configured/)
  })

  it('useCallerRole defaults to the lower-privilege "user" role in dev (fail-closed)', async () => {
    process.env.NODE_ENV = 'test'
    delete process.env.NEXT_PUBLIC_DEV_AS_ADMIN
    const { useCallerRole } = await freshImport()
    expect(useCallerRole()).toBe('user')
  })

  it('useCallerRole flips to "admin" ONLY when NEXT_PUBLIC_DEV_AS_ADMIN=1 is explicitly set', async () => {
    process.env.NODE_ENV = 'test'
    process.env.NEXT_PUBLIC_DEV_AS_ADMIN = '1'
    const { useCallerRole } = await freshImport()
    expect(useCallerRole()).toBe('admin')
  })

  it('useCallerRole THROWS in production regardless of the dev-as-admin opt-in', async () => {
    process.env.NODE_ENV = 'production'
    process.env.NEXT_PUBLIC_DEV_AS_ADMIN = '1'
    const { useCallerRole } = await freshImport()
    expect(() => useCallerRole()).toThrow(/Identity provider not configured/)
  })

  it('warns exactly once per module instance, not on every call', async () => {
    process.env.NODE_ENV = 'test'
    const { useCallerId } = await freshImport()
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    useCallerId()
    useCallerId()
    useCallerId()
    expect(warnSpy).toHaveBeenCalledTimes(1)
  })
})

describe('normalizeUserId — trim + null-safe coercion', () => {
  it('trims whitespace', async () => {
    const { normalizeUserId } = await freshImport()
    expect(normalizeUserId('  abc  ')).toBe('abc')
  })

  it('returns empty string for null/undefined', async () => {
    const { normalizeUserId } = await freshImport()
    expect(normalizeUserId(null)).toBe('')
    expect(normalizeUserId(undefined)).toBe('')
  })
})

// P3-98 — JURISDICTION OF THIS CONTRACT. sameUser is a DISPLAY/UI identity helper, NOT an
// authorization comparator. The trim asserted below is deliberate and stays: it exists so a
// "you" chip or a "you are the actor" label still lights up for a fork-receiver session
// hook that returns a padded id, which has no authorization consequence.
//
// It must NOT be read as "the catalog folds identity for authorization". Any comparison
// that gates a mutation or renders a backend verdict as fact uses the EXACT mirror
// `sameId` exported from templates/L0/fork-receiver-kit/authorized-actions.ts (bare
// equality, matching ApprovalActionGuards / ApprovalService.validateApprovers). The exact
// leg is pinned by frontend/tests/authz-action-parity.vitest.ts (mixed-case / padded
// golden rows) and, for the L4 pages that consume it, by
// frontend/tests/approval-detail-view.vitest.tsx.
describe('sameUser — blank ids never match (polymorphic-ownership pitfall)', () => {
  it('matches equal, whitespace-trimmed ids — display jurisdiction, see the note above', async () => {
    const { sameUser } = await freshImport()
    expect(sameUser('user-1', 'user-1')).toBe(true)
    expect(sameUser(' user-1 ', 'user-1')).toBe(true)
  })

  it('does not match differing ids', async () => {
    const { sameUser } = await freshImport()
    expect(sameUser('user-1', 'user-2')).toBe(false)
  })

  it('never matches when either side is blank — two anonymous callers are NOT equal', async () => {
    const { sameUser } = await freshImport()
    expect(sameUser('', '')).toBe(false)
    expect(sameUser(null, null)).toBe(false)
    expect(sameUser(undefined, undefined)).toBe(false)
    expect(sameUser('user-1', '')).toBe(false)
    expect(sameUser('', 'user-1')).toBe(false)
  })
})
