import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/prefer-functional-setstate.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

test('ax/prefer-functional-setstate — RuleTester suite', () => {
  tester.run('ax/prefer-functional-setstate', rule, {
    valid: [
      // Functional form already
      `
        function C() {
          const [count, setCount] = useState(0)
          const inc = () => setCount((c) => c + 1)
        }
      `,
      // Static value
      `
        function C() {
          const [count, setCount] = useState(0)
          const reset = () => setCount(0)
        }
      `,
      // Value from argument (doesn't reference state name)
      `
        function C() {
          const [name, setName] = useState('')
          const onChange = (newName) => setName(newName)
        }
      `,
    ],
    invalid: [
      // Directly references state variable
      {
        code: `
          function C() {
            const [count, setCount] = useState(0)
            const inc = () => setCount(count + 1)
          }
        `,
        errors: [{ messageId: 'preferFunctional' }],
      },
      // Array spread reference
      {
        code: `
          function C() {
            const [items, setItems] = useState([])
            const add = (x) => setItems([...items, x])
          }
        `,
        errors: [{ messageId: 'preferFunctional' }],
      },
      // Object spread reference
      {
        code: `
          function C() {
            const [user, setUser] = useState({})
            const patch = (k, v) => setUser({ ...user, [k]: v })
          }
        `,
        errors: [{ messageId: 'preferFunctional' }],
      },
    ],
  })
})
