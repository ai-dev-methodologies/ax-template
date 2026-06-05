// apps/consumer — ESLint v9 flat config.
//
// Re-exports the repo-root flat config, which already scopes per-persona apps
// (apps/**) to the ENFORCED-reuse rule set: ax/no-app-local-ui-primitives is
// ERROR here. Keeping this app on the same config guarantees the boundary rule
// is applied identically whether ESLint runs from the repo root
// (`eslint apps/consumer`) or from inside this app (`npm run lint -w
// @ax/app-consumer`).
import rootConfig from '../../eslint.config.mjs';

export default rootConfig;
