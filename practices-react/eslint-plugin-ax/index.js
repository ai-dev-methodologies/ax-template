/**
 * @ax/eslint-plugin-ax
 *
 * Custom ESLint plugin pairing with the practices-react/ catalog.
 * Each rule name matches the corresponding practices-react/rules/<id>.md file
 * via the rule's `verification.rule_id` frontmatter field.
 */

import reactAsyncParallel from './rules/react-async-parallel.js'
import noBroadBarrelImports from './rules/no-broad-barrel-imports.js'
import noFalsyNumericRender from './rules/no-falsy-numeric-render.js'
import noArrayIncludesInLoop from './rules/no-array-includes-in-loop.js'
import noArrayMutateOnState from './rules/no-array-mutate-on-state.js'
import preferFunctionalSetState from './rules/prefer-functional-setstate.js'
import noInlineComponentDefinition from './rules/no-inline-component-definition.js'
import noAppLocalUiPrimitives from './rules/no-app-local-ui-primitives.js'
import noCrossFeatureDeepImport from './rules/no-cross-feature-deep-import.js'
import noUpwardLayerImport from './rules/no-upward-layer-import.js'
import noFeatureInternalImport from './rules/no-feature-internal-import.js'
import noRouteClientDataFetching from './rules/no-route-client-data-fetching.js'
import noServerStateInLocalState from './rules/no-server-state-in-local-state.js'
import noGodRoute from './rules/no-god-route.js'
import noCallerIdentityFromProps from './rules/no-caller-identity-from-props.js'

const plugin = {
  meta: {
    name: '@ax/eslint-plugin-ax',
    version: '0.0.2',
  },
  rules: {
    'react-async-parallel': reactAsyncParallel,
    'no-broad-barrel-imports': noBroadBarrelImports,
    'no-falsy-numeric-render': noFalsyNumericRender,
    'no-array-includes-in-loop': noArrayIncludesInLoop,
    'no-array-mutate-on-state': noArrayMutateOnState,
    'prefer-functional-setstate': preferFunctionalSetState,
    'no-inline-component-definition': noInlineComponentDefinition,
    'no-app-local-ui-primitives': noAppLocalUiPrimitives,
    'no-cross-feature-deep-import': noCrossFeatureDeepImport,
    'no-upward-layer-import': noUpwardLayerImport,
    'no-feature-internal-import': noFeatureInternalImport,
    'no-route-client-data-fetching': noRouteClientDataFetching,
    'no-server-state-in-local-state': noServerStateInLocalState,
    'no-god-route': noGodRoute,
    'no-caller-identity-from-props': noCallerIdentityFromProps,
  },
  configs: {},
}

// ESLint 9 flat-config recommended preset
plugin.configs.recommended = {
  plugins: { ax: plugin },
  rules: {
    'ax/react-async-parallel': 'error',
    'ax/no-broad-barrel-imports': 'error',
    'ax/no-falsy-numeric-render': 'error',
    'ax/no-array-includes-in-loop': 'error',
    'ax/no-array-mutate-on-state': 'error',
    'ax/prefer-functional-setstate': 'error',
    'ax/no-inline-component-definition': 'error',
    'ax/no-app-local-ui-primitives': 'error',
    'ax/no-cross-feature-deep-import': 'error',
    'ax/no-upward-layer-import': 'error',
    'ax/no-feature-internal-import': 'error',
    'ax/no-route-client-data-fetching': 'error',
    // BACKLOG P2-2 (2026-06-24): promoted warn→error after the P2-12 decomposition
    // wave proved 0 violations across all 6 reference apps under --max-warnings 0.
    'ax/no-server-state-in-local-state': 'error',
    'ax/no-god-route': 'error',
    // Shipped at error (not warn→promote). The detector's soundness boundary is PROVABLE
    // IMMUTABILITY + PROVENANCE (codex round-6): a binding's static value is trusted as a
    // source ONLY when the binding is provably immutable (a PARAMETER never reassigned; a
    // CONST OBJECT never property-mutated — `const` freezes the binding, not the object),
    // decided from ESLint scope references, not flow analysis. IN scope = provably-immutable
    // parameter sources + provably-immutable const variable/projection resolution +
    // ROUTER-IMPORTED source hooks; OUT of scope (documented) = mutable let/var flows, any
    // reassigned parameter or property-mutated const object (dropped conservatively → a
    // documented false-negative, never a false positive), mutation via a separate alias or
    // an arbitrary called helper, interprocedural helper-indirection, spread-into-object
    // projection, non-router-imported / locally-defined hooks, and identity-named-filter
    // callbacks. The client is untrusted, so the AUTHORITATIVE BFLA control is the BACKEND
    // authz + the sibling BE rule caller-authentication-only-no-userid-param; this FE lint is
    // defense-in-depth. A standalone NON-VACUOUS Linter-API sweep (TS parser + non-vacuity
    // canary) across the 6 apps + frontend/src + frontend/packages + templates/L1 +
    // templates/L4 is false-positive-free: a local `const props = auth()`, a `let`-mutated
    // alias, a reassigned param, a property-mutated const object, and a locally-defined
    // `useParams` are all correctly NOT flagged. (The `cd frontend && npm run lint` gate is
    // scoped to frontend/; templates/L1+L4 are swept out-of-band by the standalone sweep.)
    'ax/no-caller-identity-from-props': 'error',
  },
}

export default plugin
