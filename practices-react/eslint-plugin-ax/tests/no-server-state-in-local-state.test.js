import { RuleTester } from 'eslint'
import test from 'node:test'
import rule from '../rules/no-server-state-in-local-state.js'

const tester = new RuleTester({
  languageOptions: { ecmaVersion: 2024, sourceType: 'module' },
})

test('ax/no-server-state-in-local-state — RuleTester suite', () => {
  tester.run('ax/no-server-state-in-local-state', rule, {
    valid: [
      { code: `const [n, setN] = useState(0)` },
      { code: `const [d, setD] = useState(props.data)` },
      // intermediate variable — explicitly NOT caught (documented honest limit)
      { code: `const r = useSWR('/x'); const [d, setD] = useState(r.data)` },
    ],
    invalid: [
      {
        code: `const [d, setD] = useState(useSWR('/api/me').data)`,
        errors: [{ messageId: 'serverStateCopied' }],
      },
      {
        code: `const [d, setD] = useState(useQuery({ queryKey: ['x'] }).data)`,
        errors: [{ messageId: 'serverStateCopied' }],
      },
    ],
  })
})
