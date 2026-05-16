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
  },
  configs: {},
}

// ESLint 9 flat-config recommended preset
plugin.configs.recommended = {
  plugins: { ax: plugin },
  rules: {
    'ax/react-async-parallel': 'warn',
    'ax/no-broad-barrel-imports': 'warn',
    'ax/no-falsy-numeric-render': 'error',
    'ax/no-array-includes-in-loop': 'warn',
    'ax/no-array-mutate-on-state': 'error',
    'ax/prefer-functional-setstate': 'warn',
    'ax/no-inline-component-definition': 'error',
  },
}

export default plugin
