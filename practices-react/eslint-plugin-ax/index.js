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
  },
}

export default plugin
