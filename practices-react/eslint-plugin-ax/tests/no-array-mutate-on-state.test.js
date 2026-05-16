import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-array-mutate-on-state.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

test('ax/no-array-mutate-on-state — RuleTester suite', () => {
  tester.run('ax/no-array-mutate-on-state', rule, {
    valid: [
      // Local variable mutation — fine
      `
        function f() {
          const local = [3, 1, 2]
          local.sort()
        }
      `,
      // Immutable variant on prop — fine
      `
        function UserList({ users }) {
          const sorted = users.toSorted()
          return sorted
        }
      `,
      // Spread before sort — fine
      `
        function UserList({ users }) {
          const sorted = [...users].sort()
          return sorted
        }
      `,
    ],
    invalid: [
      // .sort on a prop
      {
        code: `
          function UserList({ users }) {
            const sorted = users.sort()
            return sorted
          }
        `,
        errors: [{ messageId: 'mutateOnState' }],
      },
      // .reverse on useState array
      {
        code: `
          function App() {
            const [items, setItems] = useState([])
            const reversed = items.reverse()
          }
        `,
        errors: [{ messageId: 'mutateOnState' }],
      },
      // .splice on a prop
      {
        code: `
          function Cart({ lineItems }) {
            lineItems.splice(0, 1)
          }
        `,
        errors: [{ messageId: 'mutateOnState' }],
      },
    ],
  })
})
