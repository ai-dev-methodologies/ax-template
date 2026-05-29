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
      // .push / index-assign on a LOCAL array — fine (not prop/state)
      `
        function f() {
          const local = []
          local.push(1)
          local[0] = 9
        }
      `,
      // Immutable add on a prop — fine
      `
        function UserList({ users }) {
          const next = [...users, 1]
          return next
        }
      `,
      // .fill on a destructured NON-array param (Playwright page) — must NOT
      // fire: collision-prone methods are useState-only (FDW2 regression fix).
      `
        async function test({ page }) {
          await page.fill('#email', 'a@b.com')
        }
      `,
      // .push on a bare param (could be anything) — not flagged (state-only).
      `
        function handler({ stack }) {
          stack.push(1)
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
      // .push on useState array (no immutable variant → mutateMethodOnState)
      {
        code: `
          function App() {
            const [items, setItems] = useState([])
            items.push(1)
          }
        `,
        errors: [{ messageId: 'mutateMethodOnState' }],
      },
      // .pop on a useState array (state-only for in-place mutators)
      {
        code: `
          function Cart() {
            const [lineItems, setLineItems] = useState([])
            lineItems.pop()
          }
        `,
        errors: [{ messageId: 'mutateMethodOnState' }],
      },
      // .unshift on a useState array
      {
        code: `
          function List() {
            const [rows, setRows] = useState([])
            rows.unshift({})
          }
        `,
        errors: [{ messageId: 'mutateMethodOnState' }],
      },
      // index assignment on useState array — arr[i] = v
      {
        code: `
          function App() {
            const [items, setItems] = useState([])
            items[0] = 9
          }
        `,
        errors: [{ messageId: 'assignIndexOnState' }],
      },
      // index assignment on a prop
      {
        code: `
          function Grid({ cells }) {
            cells[2] = null
          }
        `,
        errors: [{ messageId: 'assignIndexOnState' }],
      },
    ],
  })
})
