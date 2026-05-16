import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-falsy-numeric-render.js'

const tester = new RuleTester({
  languageOptions: {
    ecmaVersion: 2024,
    sourceType: 'module',
    parserOptions: { ecmaFeatures: { jsx: true } },
  },
})

test('ax/no-falsy-numeric-render — RuleTester suite', () => {
  tester.run('ax/no-falsy-numeric-render', rule, {
    valid: [
      // Real boolean — safe
      'const C = ({ canEdit }) => <div>{canEdit && <button/>}</div>',
      // Comparison — safe
      'const C = ({ count }) => <div>{count > 0 && <span/>}</div>',
      // Boolean cast — safe
      'const C = ({ count }) => <div>{Boolean(count) && <span/>}</div>',
      // ! unary — safe
      'const C = ({ loaded }) => <div>{!loaded && <span/>}</div>',
      // Equality — safe
      'const C = ({ mode }) => <div>{mode === "ok" && <span/>}</div>',
    ],
    invalid: [
      // count (looks numeric) && JSX
      {
        code: 'const C = ({ count }) => <div>{count && <span/>}</div>',
        errors: [{ messageId: 'falsyNumeric' }],
      },
      // length (looks numeric) && JSX
      {
        code: 'const C = ({ items }) => <div>{items.length && <ul/>}</div>',
        errors: [{ messageId: 'falsyNumeric' }],
      },
      // Numeric literal && JSX (extreme case)
      {
        code: 'const C = () => <div>{0 && <span/>}</div>',
        errors: [{ messageId: 'falsyNumeric' }],
      },
      // Arithmetic && JSX
      {
        code: 'const C = ({ a, b }) => <div>{a + b && <span/>}</div>',
        errors: [{ messageId: 'falsyNumeric' }],
      },
    ],
  })
})
