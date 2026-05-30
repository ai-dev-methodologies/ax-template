// Vitest stub for `next/navigation`.
//
// The kit unit tests import repo-root `templates/L0/...` files (copied into a
// fork's src/ on adoption). Those hook files statically `import` from
// `next/navigation`, but vite resolves bare specifiers from the project root
// (frontend/) — and for a file OUTSIDE that root it cannot find next, so the
// module graph fails to build before any test runs. This stub gives the
// specifier a resolvable target. The hooks are inert: kit PURE exports under
// test (e.g. `listStateToQuery`) never call them, and component tests that DO
// exercise the hooks `vi.mock('next/navigation')` with their own behavior,
// which overrides this stub at runtime.
export const useRouter = () => ({
  push: () => {},
  replace: () => {},
  back: () => {},
  forward: () => {},
  refresh: () => {},
  prefetch: () => {},
})
export const useSearchParams = () => new URLSearchParams()
export const usePathname = () => '/'
export const useParams = () => ({})
export const useSelectedLayoutSegment = () => null
export const useSelectedLayoutSegments = () => []
export const redirect = () => {
  throw new Error('next/navigation redirect() called in a vitest stub')
}
export const notFound = () => {
  throw new Error('next/navigation notFound() called in a vitest stub')
}
