import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-app-local-ui-primitives.js'

const tester = new RuleTester({
  languageOptions: {
    ecmaVersion: 2024,
    sourceType: 'module',
    parserOptions: { ecmaFeatures: { jsx: true } },
  },
})

const APP_FILE = '/repo/frontend/apps/enterprise/src/Button.tsx'
const SHELL_FILE = '/repo/frontend/src/components/ui/button.tsx'

test('ax/no-app-local-ui-primitives — RuleTester suite', () => {
  tester.run('ax/no-app-local-ui-primitives', rule, {
    valid: [
      // App file importing the primitive from the shared catalog — the correct pattern.
      {
        code: `
          import { Button } from '@ax/ui'
          export function LoginCard() { return <Button>Sign in</Button> }
        `,
        filename: APP_FILE,
      },
      // App file importing a composed block from @ax/blocks — fine.
      {
        code: `
          import { StatusBadge } from '@ax/blocks'
          export function Row() { return <StatusBadge status="ok" /> }
        `,
        filename: APP_FILE,
      },
      // App file re-exporting the primitive from the catalog — fine.
      {
        code: `export { Button } from '@ax/ui'`,
        filename: APP_FILE,
      },
      // App-local function named Button that does NOT return JSX (a helper) — fine.
      {
        code: `
          function Button(n) { return n * 2 }
          export const x = Button(2)
        `,
        filename: APP_FILE,
      },
      // App-local component with a NON-primitive name — fine (only catalog names are reserved).
      {
        code: `export function LoginPanel() { return <div>hi</div> }`,
        filename: APP_FILE,
      },
      // The SAME bespoke Button, but in the root web-shell app (not under apps/**) — exempt.
      {
        code: `export function Button() { return <button /> }`,
        filename: SHELL_FILE,
      },
      // Root web-shell importing its own components/ui — exempt (predates the catalog).
      {
        code: `import { Button } from './components/ui/button'`,
        filename: SHELL_FILE,
      },
    ],
    invalid: [
      // App-local component DEFINITION named like a catalog primitive (function decl).
      {
        code: `export function Button() { return <button>click</button> }`,
        filename: APP_FILE,
        errors: [{ messageId: 'localComponentDefinition' }],
      },
      // App-local arrow component named Card returning JSX.
      {
        code: `const Card = () => <div className="card" />`,
        filename: APP_FILE,
        errors: [{ messageId: 'localComponentDefinition' }],
      },
      // App-local forwardRef factory assigned to a primitive name.
      {
        code: `
          import { forwardRef } from 'react'
          const Input = forwardRef((props, ref) => <input ref={ref} {...props} />)
          export { Input }
        `,
        filename: APP_FILE,
        // One for the forwardRef definition, one for the re-export of a primitive.
        errors: [
          { messageId: 'localComponentDefinition' },
          { messageId: 'localComponentExport' },
        ],
      },
      // App-local export aliasing some component to a catalog primitive name.
      {
        code: `
          function MyThing() { return <span /> }
          export { MyThing as Badge }
        `,
        filename: APP_FILE,
        errors: [{ messageId: 'localComponentExport' }],
      },
      // App importing a local components/ui module (relative).
      {
        code: `import { Button } from '../../components/ui/button'`,
        filename: APP_FILE,
        errors: [{ messageId: 'localComponentsUiImport' }],
      },
    ],
  })
})
