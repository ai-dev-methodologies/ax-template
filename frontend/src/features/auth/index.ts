// auth — feature-level published barrel (frontend decomposition spec §7 Phase 1).
//
// This is the auth feature's PUBLIC surface. Code outside `features/auth/` imports
// the feature through this barrel (or a slice barrel `@/features/auth/<slice>`),
// never a slice's internal files (enforced by ax/no-feature-internal-import and
// ax/no-cross-feature-deep-import). Slices re-export their public API here as they
// are built out (today the slices are skeletal placeholders).
export * from './login'
export * from './signup'
export * from './verify-email-result'
export * from './protected-route-guard'
