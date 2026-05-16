import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-inline-component-definition.js'

const tester = new RuleTester({
  languageOptions: {
    ecmaVersion: 2024,
    sourceType: 'module',
    parserOptions: { ecmaFeatures: { jsx: true } },
  },
})

test('ax/no-inline-component-definition — RuleTester suite', () => {
  tester.run('ax/no-inline-component-definition', rule, {
    valid: [
      // Component defined at module scope — fine
      `
        function Avatar({ src }) {
          return <img src={src} />
        }
        function UserProfile({ user }) {
          return <Avatar src={user.avatar} />
        }
      `,
      // Lowercase inline helper called as a function — fine
      `
        function ItemList({ items }) {
          const renderItem = (it) => <li key={it.id}>{it.name}</li>
          return <ul>{items.map(renderItem)}</ul>
        }
      `,
      // Inner non-component (returns non-JSX) — fine
      `
        function App() {
          function compute(n) { return n * 2 }
          return <div>{compute(2)}</div>
        }
      `,
    ],
    invalid: [
      // Capitalized inner component returning JSX
      {
        code: `
          function UserProfile({ user }) {
            const Avatar = () => <img src={user.avatar} />
            return <div><Avatar /></div>
          }
        `,
        errors: [{ messageId: 'innerComponent' }],
      },
      // Function declaration inside another component
      {
        code: `
          function UserProfile({ user }) {
            function Stats() {
              return <div>{user.posts}</div>
            }
            return <Stats />
          }
        `,
        errors: [{ messageId: 'innerComponent' }],
      },
    ],
  })
})
