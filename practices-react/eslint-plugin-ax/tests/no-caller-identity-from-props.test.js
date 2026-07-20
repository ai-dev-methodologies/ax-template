import { RuleTester } from 'eslint'
import test from 'node:test'
import { createRequire } from 'node:module'
import rule from '../rules/no-caller-identity-from-props.js'

const tester = new RuleTester({
  languageOptions: {
    ecmaVersion: 2024,
    sourceType: 'module',
    // JSX enabled so the round-11 render-only valid case (`<span>{props.userId}</span>`)
    // parses under the default espree parser.
    parserOptions: { ecmaFeatures: { jsx: true } },
  },
})

test('ax/no-caller-identity-from-props — RuleTester suite', () => {
  tester.run('ax/no-caller-identity-from-props', rule, {
    valid: [
      // Correct pattern: identity derives from the caller-id hook / session.
      { code: `const callerId = useCallerId(); api.get(callerId)` },
      { code: `function Orders() { const userId = useCallerId(); return api.get(userId) }` },
      // Clerk-style `const { userId } = auth()` — NOT props/params/searchParams, so
      // this is the CORRECT session-derived pattern and must not be flagged.
      { code: `function Orders() { const { userId } = auth(); return api.get(userId) }` },
      // Tainted param exists, but never flows into a data call — no violation.
      { code: `function Widget({ userId }) { return React.createElement('span', null, userId) }` },
      // A plain (non-identity) prop passed to a data call is fine.
      { code: `function Orders({ pageSize }) { return api.get(pageSize) }` },
      // Out-of-scope, documented limit: identity read through an intermediate helper
      // function is NOT traced (no interprocedural data-flow analysis).
      {
        code: `function readId(p) { return p.userId }
function Orders(props) { const uid = readId(props); return api.get(uid) }`,
      },
      // Out-of-scope, documented limit: a dynamically-computed member key is not traced.
      { code: `function Orders(props) { const key = 'userId'; return api.get(props[key]) }` },
      // Finding 2 (scope-aware taint): a SAFE `const userId = useCallerId()` must stay
      // clean even when a SIBLING function binds a props-derived `userId`. Here the
      // sibling's tainted `userId` is only rendered (no authz call), so the whole file is
      // valid — proving taint no longer leaks across function boundaries by bare name.
      {
        code: `function Profile({ userId }) { return React.createElement('span', null, userId) }
function Orders() { const userId = useCallerId(); return api.get(userId) }`,
      },
      // Finding 2 (shadowing — HARDENED): an inner binding that re-declares a name safely
      // wins over an outer tainted binding of the same name. The inner call is now an
      // AUTHZ-RELEVANT data call (`api.get`), so this case actually proves the inner
      // safe `useCallerId()` binding is NOT tainted by the outer `props.userId` — the
      // prior `renderName(userId)` was a non-data call that passed regardless of taint.
      {
        code: `function Outer(props) {
  const userId = props.userId
  function Inner() { const userId = useCallerId(); return api.get(userId) }
  return Inner
}`,
      },
      // Finding 1 (safe wrappers must NOT false-positive): a session-derived identity
      // buried inside the SAME wrapper/combining nodes the rule now recurses through must
      // stay clean — recursion looks for a TAINTED source, not merely "wrapped".
      // Nullish-coalescing of two session sources.
      { code: `function Orders() { return api.get(useCallerId() ?? fallbackFromSession()) }` },
      // Ternary between two session sources.
      { code: `function Orders(cond) { return api.get(cond ? useCallerId() : sessionId()) }` },
      // Template-literal interpolation of a session source.
      { code: `function Orders() { return api.get(\`/x?u=\${useCallerId()}\`) }` },
      // String concatenation with a session source (BinaryExpression, no taint).
      { code: `function Orders() { return api.get('/orders?u=' + useCallerId()) }` },
      // Optional-chained SAFE object (not props/params/searchParams) — auth() session.
      { code: `function Orders() { const session = auth(); return api.get(session?.userId) }` },

      // ---- Round-3 (codex): unified taint via resolved-const initializer + spreads.
      // The safe counterparts of the round-3 invalid flows must NOT false-positive — the
      // resolved-const path runs the SAME exhaustive recursion, which still looks for a
      // TAINTED source, not merely "wrapped/resolved".
      // Safe init resolved through an intermediate const.
      { code: `function Orders() { const uid = useCallerId(); return api.get(uid) }` },
      // Safe object SPREAD — session identity inside a spread must stay clean.
      { code: `function Orders() { return api.get({ ...{ userId: useCallerId() } }) }` },
      // Safe `??` initializer through an intermediate const.
      { code: `function Orders() { const uid = useCallerId() ?? fallback(); return api.get(uid) }` },
      // Chained safe consts (`const a = useCallerId(); const b = a`) must not taint via
      // the resolved-initializer recursion.
      { code: `function Orders() { const a = useCallerId(); const b = a; return api.get(b) }` },
      // Native fetch sink with a SESSION identity in a URL template — must not
      // false-positive on safe interpolations.
      { code: `function Orders() { return fetch(\`/api?u=\${useCallerId()}\`) }` },
      // Combining source position, but the destructured object is a SAFE session, not a
      // source object — `const { userId } = auth() ?? {}` stays clean.
      { code: `function Orders() { const { userId } = auth() ?? {}; return api.get(userId) }` },
      // Assignment recursion is RIGHT-only: a write TARGET `props.userId = x` passes the
      // VALUE `x` (not the identity), so it must NOT flag. Locks the right-only semantics
      // against a future regression to left-recursion (which would false-positive here).
      { code: `function Orders(props, x) { return api.get(props.userId = x) }` },

      // ---- Round-4 (codex): PROVENANCE-based source detection, not name-based. A LOCAL
      // binding that merely reuses a source name is NOT a source object — its init is not a
      // source shape — so a member read on it must stay clean.
      // THE FALSE POSITIVE codex proved: `const props = auth()` is a session-derived local,
      // NOT the component's props parameter. Must NOT flag (previously flagged by name).
      { code: `function Orders() { const props = auth(); return api.get(props.userId) }` },
      // Same class, `params` name reused for a locally-parsed value.
      { code: `function Orders() { const params = parseThing(); return api.get(params.userId) }` },
      // Same class for the `searchParams.get('userId')` leaf: a local Map/URLSearchParams
      // that reuses the name is not a caller-supplied source → `.get('userId')` stays clean.
      { code: `function Orders() { const searchParams = makeMap(); return api.get(searchParams.get('userId')) }` },
      // Provenance for a source-hook alias must still be a SOURCE — its safe counterpart
      // (session-derived alias) must NOT flag: `const p = auth(); p.userId` is clean.
      { code: `function Orders() { const p = auth(); return api.get(p.userId) }` },
      // Static container projection, SAFE value — projecting a session identity off a const
      // object literal must NOT flag (projection taints only when the slot value is tainted).
      { code: `function Orders() { const q = { userId: useCallerId() }; return api.get(q.userId) }` },
      // Reaching assignment, SAFE value — a `let` reassigned to a session source stays clean.
      { code: `function Orders() { let uid; uid = useCallerId(); return api.get(uid) }` },

      // ---- Round-5 (codex): the IMMUTABILITY boundary. A heuristic lint cannot soundly do
      // flow-sensitive taint through MUTABLE (`let`/`var`) bindings — the value at the use
      // site depends on statement order / reassignments. So `let`/`var` bindings are NOT
      // taint-tracked at all; only `const` (single definitive initializer) is. These are the
      // codex round-5 false positives, now ELIMINATED (must NOT flag).
      // (1) `let` tainted-init OVERWRITTEN before use — safe, previously false-positived.
      { code: `function Orders(props) { let uid = props.userId; uid = auth(); return api.get(uid) }` },
      // (2) `let` safe-init, tainted WRITE AFTER the use — the write does not reach the use;
      // flow-insensitive taint wrongly flagged this. Now out of scope (let), no diagnostic.
      { code: `function Orders(props) { let uid = auth(); api.get(uid); uid = props.userId }` },
      // (3) `let` ALIAS of props, overwritten to a safe value before the read — safe.
      { code: `function Orders(props) { let p = props; p = auth(); return api.get(p.userId) }` },
      // (4) reaching-assignment into a `let` from a source — now an OUT-OF-SCOPE miss (was
      // round-4 invalid). Mutable-variable flows require flow-sensitivity the lint omits; the
      // authoritative backend authz control covers this. Must NOT flag.
      { code: `function Orders(props) { let uid; uid = props.userId; return api.get(uid) }` },
      // (5) `let` reassigned from a safe init to a source — same out-of-scope miss.
      { code: `function Orders(props) { let uid = useCallerId(); uid = props.userId; return api.get(uid) }` },
      // (6) SPELLING-based hook detection eliminated: a LOCALLY-DEFINED `useParams` is NOT a
      // router source object (detection is by IMPORT PROVENANCE). `p.userId` stays clean.
      {
        code: `function useParams() { return auth() }
function Orders() { const p = useParams(); return api.get(p.userId) }`,
      },
      // (7) a same-named `useParams` IMPORTED from a NON-router package is not a source.
      {
        code: `import { useParams } from '@/lib/local-hooks'
function Orders() { const p = useParams(); return api.get(p.userId) }`,
      },
      // (8) `const` binding of a LOCALLY-DEFINED useParams result — provenance (not spelling)
      // means this is clean even though the name matches.
      {
        code: `function useSearchParams() { return makeMap() }
function Orders() { const sp = useSearchParams(); return api.get(sp.get('userId')) }`,
      },

      // ---- Round-6 (codex): PROVABLE IMMUTABILITY within the retained const/param scope. The
      // taint was flow-INSENSITIVE, so a binding OVERWRITTEN to a SAFE value AFTER being tainted
      // still false-positived. A binding's static value is now trusted ONLY when the binding is
      // provably immutable — a PARAMETER never reassigned, a CONST OBJECT never property-mutated.
      // These are the round-6 false positives, now ELIMINATED (must NOT flag).
      // (1) CONST OBJECT PROPERTY MUTATION — the slot is overwritten to a session value before
      // the projection read; the stale initializer slot must no longer be trusted.
      { code: `function Orders(props) { const q = { userId: props.userId }; q.userId = useCallerId(); return api.get(q.userId) }` },
      // (1b) same mutation, but the WHOLE object is passed — the resolved-const object path must
      // also drop the stale initializer.
      { code: `function Orders(props) { const q = { userId: props.userId }; q.userId = useCallerId(); return api.get(q) }` },
      // (1c) COMPUTED property write (`q['userId'] = …`) is a mutation too.
      { code: `function Orders(props) { const q = { userId: props.userId }; q['userId'] = useCallerId(); return api.get(q.userId) }` },
      // (1d) Object.assign(q, …) mutates q in place — drop the projection.
      { code: `function Orders(props) { const q = { userId: props.userId }; Object.assign(q, { userId: useCallerId() }); return api.get(q.userId) }` },
      // (1e) NESTED-object mutation reaches the tainted slot — the whole-object resolution drops.
      { code: `function Orders(props) { const q = { inner: { userId: props.userId } }; q.inner.userId = useCallerId(); return api.get(q) }` },
      // (1f) delete removes the slot — mutation, drop.
      { code: `function Orders(props) { const q = { userId: props.userId }; delete q.userId; return api.get(q.userId) }` },
      // (1g) `q.userId++` update is a mutation — drop.
      { code: `function Orders(props) { const q = { userId: props.userId }; q.userId++; return api.get(q.userId) }` },
      // (1h) ARRAY element write, whole-array passed — the array-literal init path drops.
      { code: `function Orders(props) { const arr = [props.userId]; arr[0] = useCallerId(); return api.get(arr) }` },
      // (2) PARAMETER REASSIGNMENT — a source param overwritten to a session object before use.
      { code: `function Orders(props) { props = { userId: useCallerId() }; return api.get(props.userId) }` },
      // (2b) DESTRUCTURED identity param reassigned to a session value before use.
      { code: `function Orders({ userId }) { userId = useCallerId(); return api.get(userId) }` },

      // ---- Round-7 (codex): DESTRUCTURING-ASSIGNMENT-TARGET mutation/reassignment. A write
      // via a plain assignment (`q.userId = …`) was already detected as a mutation, but the
      // SAME write reached through a destructuring-ASSIGNMENT pattern target
      // (`({ userId: q.userId } = safe)`, `[q.userId] = safe`) was missed — ESLint scope
      // analysis treats the member's OBJECT there as a plain READ reference, not a write, so
      // the const object stayed (wrongly) trusted and the projection/whole-object use kept
      // false-positive-flagging. Now detected via `isPatternAssignmentTarget` — must NOT flag.
      // (3) Object-destructuring-assignment MEMBER target mutates `q.userId` — projection use.
      { code: `function Orders(props) { const q = { userId: props.userId }; ({ userId: q.userId } = safe); return api.get(q) }` },
      // (3b) same, but the identity is READ back through the mutated slot before the call.
      { code: `function Orders(props) { const q = { userId: props.userId }; ({ userId: q.userId } = safe); return api.get(q.userId) }` },
      // (4) Array-destructuring-assignment MEMBER target mutates `q.userId`.
      { code: `function Orders(props) { const q = { userId: props.userId }; [q.userId] = [safe]; return api.get(q.userId) }` },
      // (5) PARAMETER reassigned via a destructuring assignment (not a plain `props = …`) —
      // an array-pattern reassignment of the whole `props` binding before use.
      { code: `function Orders(props) { [props] = [{ userId: useCallerId() }]; return api.get(props.userId) }` },
      // (5b) parameter reassigned via an OBJECT-destructuring-assignment of its own name.
      { code: `function Orders(props) { ({ props } = { props: { userId: useCallerId() } }); return api.get(props.userId) }` },
      // (6) Self-check: `Object.assign` supplies the RHS value of a destructuring-assignment
      // whose LHS target is still `q.userId` — the destructuring-target detection (not the
      // separate `Object.assign(q, …)` mutator check) must catch this.
      { code: `function Orders(props) { const q = { userId: props.userId }; ({ userId: q.userId } = Object.assign({}, safe)); return api.get(q) }` },
      // (7) Self-check: COMPUTED key in the destructuring-assignment pattern still resolves
      // the value-slot target correctly (the key `[k]` is not mistaken for the target).
      { code: `function Orders(props) { const q = { userId: props.userId }; ({ [k]: q.userId } = safe); return api.get(q) }` },
      // (8) Self-check: NESTED destructuring-assignment pattern — the target is two levels
      // deep inside the object pattern.
      { code: `function Orders(props) { const q = { userId: props.userId }; ({ a: { b: q.userId } } = safe); return api.get(q) }` },

      // ---- Round-8 (codex): a COMPARISON/relational BinaryExpression and a ternary's
      // TEST reduce to a boolean/number that cannot itself carry the caller identity — a
      // heuristic that recursed into EVERY binary operator and the ConditionalExpression
      // `.test` false-positived on ordinary React Query patterns like
      // `useQuery({ enabled: id === props.userId, queryFn })`. Now ELIMINATED (must NOT flag).
      // (1) THE reported false positive: an equality check feeding a query's `enabled` flag.
      { code: `function Orders(props, id) { return useQuery({ enabled: id === props.userId, queryFn }) }` },
      // (2) Equality result stored in a const, then passed to a data call — still just a
      // boolean, not the identity.
      { code: `function Orders(props, current) { const ok = props.userId === current; return api.get(ok) }` },
      // (3) Ternary TEST is a comparison mentioning the identity, but NEITHER branch passes
      // the identity itself to the data call — must not flag from the test.
      { code: `function Orders(props, cond) { return api.get(cond === props.userId ? doA() : doB()) }` },
      // (4) Relational comparison (`<`) used as a ternary test, non-identity branches.
      { code: `function Orders(props) { return api.get(props.userId < 10 ? a : b) }` },

      // ---- Round-9 (codex): a UnaryExpression is VALUE-DERIVING — every unary operator
      // (`! typeof void ~ + -`) yields a boolean/string/number/undefined, never the caller
      // identity, so it must NOT flag. Previously UnaryExpression shared AwaitExpression's
      // unconditional recursion and false-positived on `enabled: !!props.userId`.
      // (1) THE reported false positive: double-bang boolean feeding a query's `enabled`.
      { code: `function Orders(props) { return useQuery({ enabled: !!props.userId, queryFn }) }` },
      // (2) Single negation — boolean.
      { code: `function Orders(props) { return useQuery({ enabled: !props.userId }) }` },
      // (3) `typeof` — string.
      { code: `function Orders(props) { return api.get(typeof props.userId) }` },
      // (4) `void` — undefined.
      { code: `function Orders(props) { return api.get(void props.userId) }` },
      // (5) Bitwise NOT — number.
      { code: `function Orders(props) { return api.get(~props.userId) }` },
      // (6) Unary plus — number.
      { code: `function Orders(props) { return api.get(+props.userId) }` },
      // ---- Round-9 (codex): a SequenceExpression EVALUATES to its LAST operand only —
      // earlier operands are evaluated-and-discarded, so a non-last identity does NOT reach
      // the call. `(props.userId, safeId)` must NOT flag (previously a `.some()` over ALL
      // operands false-positived on the discarded first element).
      { code: `function Orders(props, safeId) { return api.get((props.userId, safeId)) }` },

      // ---- Round-10 (codex): AwaitExpression now propagates in `isSourceObjectExpr` (the
      // SOURCE-object side), not only in `exprHasTaint` (the VALUE side). The awaited value
      // here is NOT a source object (`fetchData()` is an arbitrary, non-router-imported call)
      // and `.id` is not even an identity key — must stay clean.
      {
        code: `async function Orders() { return api.get((await fetchData()).id) }`,
      },

      // ---- Round-11 (codex): SINK NARROWING — the realistic false positive. The former
      // DATA-VERB-NAME sink heuristic flagged identity passed POSITIONALLY to ANY function
      // whose name starts with a data-ish verb (get/find/fetch/…), catching pure
      // presentation helpers. Sinks are now the high-confidence set ONLY (data hooks /
      // fetch / HTTP clients / HTTP-verb member calls). All of these MUST NOT flag.
      // (1) THE reported false positive: a pure presentation helper.
      { code: `function UserBadge(props) { return getAvatarColor(props.userId) }` },
      // (2) Another verb-prefixed pure helper (`formatName` never matched, but lock the
      // adjacent shape: identity into an arbitrary local function).
      { code: `function Card(props) { return formatName(props.userId) }` },
      // (3) Render-only usage — no data call at all.
      { code: `function Badge(props) { return <span>{props.userId}</span> }` },
      // (4) Identity into an arbitrary non-data local function (analytics-style).
      { code: `function Track(props) { logEvent(props.userId); return null }` },
      // (5) DROPPED heuristic, documented out of scope: a bare positional local
      // data-wrapper. Whether `fetchOrders`'s body reaches fetch/axios is
      // INTERPROCEDURAL; the authoritative BFLA control is the backend authz + the BE
      // rule caller-authentication-only-no-userid-param.
      { code: `function Orders(props) { return fetchOrders(props.userId) }` },
      // (6) Same for an ORM-style member wrapper — `.findMany` is not an HTTP-verb
      // member call, so a local db wrapper is out of scope too.
      { code: `function Orders(props) { return db.orders.findMany({ where: { userId: props.userId } }) }` },

      // ---- Round-12 (codex): RECEIVER PROVENANCE — the realistic false positive the
      // round-11 sink narrowing introduced. A verb-member call (`.get`/`.post`/`.put`/
      // `.patch`/`.delete`) is a sink ONLY when its receiver is a PROVEN HTTP-client
      // object — an ordinary Map/cache/Set/store call MUST NOT flag even though the
      // method name matches.
      // (1) THE reported false positive: a Map lookup.
      { code: `function Orders(props) { return map.get(props.userId) }` },
      // (2) A cache lookup — same shape, different conventional name.
      { code: `function Orders(props) { return cache.get(props.userId) }` },
      // (3) A Set removal — `.delete` is also an HTTP verb name, but `set` is not a
      // proven HTTP-client receiver.
      { code: `function Orders(props) { return set.delete(props.userId) }` },
      // (4) An explicit `new Map()` binding, read via `.get` — still not a client.
      { code: `function Orders(props) { const m = new Map(); return m.get(props.userId) }` },
      // (5) A generic store object's `.delete` — not a proven HTTP-client receiver.
      { code: `function Orders(props) { return store.delete(props.userId) }` },
      // (6) Hardening: the CONVENTIONAL-NAME fallback is gated by non-client-value proof
      // — a local named `api` that provably resolves to a `new Map()` must NOT be
      // treated as an HTTP-client receiver just because the name matches the fallback list.
      { code: `function Orders(props) { const api = new Map(); return api.get(props.userId) }` },
      // (7) Round-13 regression guard: a NON-http local factory module (`.create()` on an
      // import from an unrelated local package) must NOT be recognized as an HTTP-client
      // factory just because the factory-METHOD shape matches — factory-root provenance
      // must resolve to a KNOWN HTTP-client package, not any import.
      {
        code: `import mk from './factory'
function Orders(props) { const t = mk.create(); return t.get(props.userId) }`,
      },
    ],
    invalid: [
      // Function's OWN parameter destructuring, used directly as an argument.
      {
        code: `function Orders({ userId }) { return api.get(userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Destructuring FROM `params` (Next.js route params), used as an argument.
      {
        code: `function Page({ params }) { const { userId } = params; return api.get(userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // `searchParams.get('userId')` assigned to a variable, then used.
      {
        code: `function Page({ searchParams }) { const uid = searchParams.get('userId'); return api.get(uid) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // `searchParams.get('userId')` used inline as the argument.
      {
        code: `function Page({ searchParams }) { return api.get(searchParams.get('userId')) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Direct `props.userId` member access passed straight into the call.
      {
        code: `function Orders(props) { return api.get(props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Bypass attempt: destructuring RENAME does not defeat the rule — taint follows
      // the extracted key ('userId'), not the local binding name ('uid').
      {
        code: `function Orders({ userId: uid }) { return api.get(uid) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Bypass attempt: computed member access with a literal key is still caught.
      {
        code: `function Orders(props) { return api.get(props['userId']) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      {
        code: `function Page({ params }) { return api.get(params['userId']) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Template-literal URL interpolation into a known query hook.
      {
        code: `function Orders({ userId }) { return useSWR(\`/api/orders?userId=\${userId}\`) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Object-argument property (top-level) — query-hook config shape (round-11 sink:
      // the former bare `findOrders({ userId })` positional wrapper is out of scope).
      {
        code: `function Orders({ userId }) { return useQuery({ where: { userId } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Object-argument property nested one level — HTTP-client body shape (round-11
      // sink: the former `db.orders.findMany({ where: { userId } })` ORM wrapper is a
      // local data-wrapper, out of scope).
      {
        code: `function Orders({ userId }) { return api.post({ where: { userId } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Other identity-name variants (across the round-11 sink kinds).
      {
        code: `function Orders({ actorId }) { return api.get(actorId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      {
        code: `function Orders({ memberId }) { return api.delete(memberId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      {
        code: `function Orders({ currentUserId }) { return axios(currentUserId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Finding 1 (false negative — honest direct flows): a member-identity interpolated
      // into a TEMPLATE-LITERAL property value of an object argument (SWR/react-query
      // config-object shape). Previously missed — object-arg values were not scanned for
      // template taint.
      {
        code: `function Orders(props) { return useSWR({ url: \`/api/orders?userId=\${props.userId}\` }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Finding 1: `searchParams.get('userId')` interpolated into a template inside an
      // object argument — same object-value-scanning gap, via the searchParams source.
      {
        code: `function Page({ searchParams }) { return useQuery({ key: \`/x?u=\${searchParams.get('userId')}\` }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Finding 1: member-identity nested DEEPER than two object levels
      // (`where: { AND: { userId } }`). Previously missed — object scan stopped at depth 2.
      {
        code: `function Orders(props) { return api.post({ where: { AND: { userId: props.userId } } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Finding 1: member-identity inside an ARRAY element of an object argument
      // (`where: { OR: [{ userId }] }`). Previously missed — arrays were not
      // traversed at all.
      {
        code: `function Orders(props) { return useQuery({ where: { OR: [{ userId: props.userId }] } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Finding 2 (scope-aware taint — faithful false-positive reproduction): two
      // functions in one file. `TaintedView` reads `props.userId` into a data call (MUST
      // flag); `SafeView` derives `userId` from `useCallerId()` (MUST NOT flag). Asserting
      // EXACTLY ONE error proves the safe sibling is not tainted by the global name.
      {
        code: `function TaintedView(props) { return api.get(props.userId) }
function SafeView() { const userId = useCallerId(); return api.get(userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps', line: 1 }],
      },

      // ---- Round-2 (codex): wrapper/combining-expression taint completeness. Each of
      // these direct honest-dev BFLA flows previously returned ZERO diagnostics because
      // exprHasTaint did not recurse through the wrapping/combining node. All MUST flag.

      // (1) Optional chaining — `props?.userId` parses as ChainExpression > MemberExpression.
      {
        code: `function Orders(props) { return api.get(props?.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (2) Optional-chained member as an object-argument value (Next.js route params).
      {
        code: `function Page({ params }) { return api.post({ where: { userId: params?.userId } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (3) Optional-chained CALL (`searchParams?.get('userId')`) as an object-arg value —
      // ChainExpression > CallExpression inside a known query hook config.
      {
        code: `function Search({ searchParams }) { return useQuery({ key: searchParams?.get("userId") }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (4) String concatenation — BinaryExpression `+` with a tainted right operand.
      {
        code: `function Orders(props) { return api.get("/orders?userId=" + props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (5) Nullish-coalescing — LogicalExpression `??` with a tainted left operand.
      {
        code: `function Orders(props) { return api.get(props.userId ?? useCallerId()) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (5b) Logical AND — LogicalExpression `&&` with a tainted right operand still
      // flows the identity value out (result of `&&` IS one of its operands).
      {
        code: `function Orders(props, isReady) { return api.get(isReady && props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Additional wrapper/combining combinations (JS-parseable) ----

      // Ternary consequent is tainted — ConditionalExpression.
      {
        code: `function Orders(props, cond) { return api.get(cond ? props.userId : fallback) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Optional-chained member buried DEEP inside object > array > object (OR shape).
      {
        code: `function Orders(props) { return api.post({ where: { OR: [{ userId: props?.userId }] } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Logical `||` inside a template literal inside a known query hook.
      {
        code: `function Orders(props) { return useSWR(\`/x?u=\${props.userId || ""}\`) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Sequence expression — comma operator, last value tainted.
      {
        code: `function Orders(props) { return api.get((noop(), props.userId)) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Await-wrapped tainted member in an async data call.
      {
        code: `async function Orders(props) { return api.get(await props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Concatenation where the tainted value is a destructured-param identity binding.
      {
        code: `function Orders({ userId }) { return api.get("/o?u=" + userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-3 (codex): the TWO taint paths unified. Previously a call arg that was
      // an IDENTIFIER resolving to `const x = <init>` only checked a RAW member/get on the
      // initializer, so any wrapped/combined/container initializer escaped. Now the
      // resolved-const path runs the SAME exhaustive `exprHasTaint(<init>)`. All MUST flag.

      // (repro #1) intermediate var, optional-chained initializer.
      {
        code: `function Orders(props) { const uid = props?.userId; return api.get(uid) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // intermediate var, string-concatenation initializer.
      {
        code: `function Orders(props) { const uid = "x" + props.userId; return api.get(uid) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // intermediate var, ternary initializer (tainted consequent).
      {
        code: `function Orders(props, cond, x) { const uid = cond ? props.userId : x; return api.get(uid) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // resolved-identifier OBJECT initializer with an optional-chained searchParams.get
      // buried inside — flows into a known query hook.
      {
        code: `function Search({ searchParams }) { const q = { where: { userId: searchParams?.get('userId') } }; return useQuery(q) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // chained consts: `const a = props.userId; const b = a` — the resolved-initializer
      // recursion follows `b → a → props.userId`.
      {
        code: `function Orders(props) { const a = props.userId; const b = a; return api.get(b) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-3 (codex): SPREADS everywhere. Object-spread and array-spread (and a
      // spread call argument) were previously skipped/undescended. All MUST flag.

      // (repro #2) object SPREAD in an object argument.
      {
        code: `function Orders(props) { return api.get({ ...{ userId: props.userId } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // ARRAY spread element.
      {
        code: `function Orders(params) { return api.get([...[params.userId]]) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Spread CALL argument (`f(...[props.userId])`).
      {
        code: `function Orders(props) { return api.get(...[props.userId]) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Nested object spread carrying a searchParams.get identity into a query hook.
      {
        code: `function Search({ searchParams }) { return useQuery({ where: { ...{ userId: searchParams.get('userId') } } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-3 (self-adversarial): SOURCE-OBJECT position follows the same flow
      // model (transparent wrappers + combiners), symmetric with the value side.

      // Combining source object in a member read: `(props ?? {}).userId`.
      {
        code: `function Orders(props) { const uid = (props ?? {}).userId; return api.get(uid) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Destructure FROM a combining source object: `const { userId } = params ?? {}`.
      {
        code: `function Page({ params }) { const { userId } = params ?? {}; return api.get(userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-3/round-11: query-string interpolation into native fetch — the
      // canonical URL data-selecting position (`?u=${props.userId}`). (The former
      // data-verb TAGGED-TEMPLATE sinks — `` getOrders`…` `` / `` db.query`…` `` — were
      // dropped with the round-11 verb heuristic; template-interpolation taint coverage
      // lives on the in-scope fetch/hook sinks instead.)
      {
        code: `function Orders(props) { return fetch(\`/api/orders?u=\${props.userId}\`) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Query-string interpolation carrying a destructured identity into fetch.
      {
        code: `function Orders({ userId }) { return fetch(\`/o?u=\${userId}\`) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-3 (self-adversarial): AssignmentExpression argument — the expression's
      // VALUE is its RHS, so it carries the tainted identity into the call.
      {
        code: `function Orders(props) { let id; return api.get(id = props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      {
        code: `function Orders(props) { let id = ""; return api.get(id += props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-4 (codex): the three intra-procedural flows the name-based detector
      // missed, now caught by provenance-aware source + taint. All MUST flag.

      // (a) ALIAS: `const p = props` — p's binding resolves (by provenance) to the props
      // PARAMETER, so `p.userId` is a source-object member read.
      {
        code: `function Orders(props) { const p = props; return api.get(p.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Alias of a Next.js `params` param, read via a computed literal key.
      {
        code: `function Page({ params }) { const p = params; return api.get(p['userId']) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (b) STATIC CONTAINER PROJECTION: `const q = { userId: props.userId }; q.userId`
      // reads a const object-literal slot whose value is tainted.
      {
        code: `function Orders(props) { const q = { userId: props.userId }; return api.get(q.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Projection carrying a searchParams.get identity into a known query hook.
      {
        code: `function Search({ searchParams }) { const q = { userId: searchParams.get('userId') }; return useQuery(q.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // NOTE: round-4's `let`-based REACHING-ASSIGNMENT invalid cases moved to VALID in
      // round-5 (the immutability boundary) — mutable-variable flows are out of scope. See
      // the round-5 valid block (cases 4 & 5).

      // ---- Round-4 (provenance model, part (b)): a ROUTER-IMPORTED source HOOK result is a
      // source object. `const params = useParams()` / `const searchParams = useSearchParams()`
      // — Next.js / React Router route inputs are caller-supplied (same IDOR class). Detection
      // is by IMPORT PROVENANCE (round-5): the hook MUST be imported from a router package.
      // MUST flag.
      {
        code: `import { useParams } from 'next/navigation'
function Orders() { const params = useParams(); return api.get(params.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      {
        code: `import { useSearchParams } from 'next/navigation'
function Orders() { const searchParams = useSearchParams(); return api.get(searchParams.get('userId')) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // React Router provenance (bare inline call, imported from react-router-dom).
      {
        code: `import { useParams } from 'react-router-dom'
function Orders() { return api.get(useParams().userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-6 (codex): the PROVABLY-IMMUTABLE counterparts STILL flag. Trust is dropped
      // only by an ACTUAL reassignment / property-mutation — a benign READ or a NON-mutating
      // pass-through must NOT disable detection (else the round-6 gate would over-suppress).
      // Const object NOT mutated — projection still tainted; a non-mutating `log(q)` is fine.
      {
        code: `function Orders(props) { const q = { userId: props.userId }; log(q); return api.get(q.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Const object NOT mutated — the WHOLE object passed still resolves the tainted slot.
      {
        code: `function Orders(props) { const q = { userId: props.userId }; return api.get(q) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Param only READ (never reassigned) — still a source, even alongside an unrelated read.
      {
        code: `function Orders(props) { const size = props.pageSize; return api.get(props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Destructured identity param only READ (never reassigned) — still tainted.
      {
        code: `function Orders({ userId }) { const label = String(userId); return api.get(userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-9 (codex): the VALUE-PRESERVING counterparts of the round-9 exclusions
      // STILL flag — the sequence fix is surgical (last-operand only), not a blanket drop.
      // (These use distinct sink/var names from the earlier value-preserving cases at
      // ~L346/351/365/385 so RuleTester accepts them as a self-contained round-9 block.)
      // A SequenceExpression whose LAST operand IS the identity carries it into the call.
      {
        code: `function Orders(props) { return api.get((logSomething(), props.userId)) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // `await` remains VALUE-PRESERVING (awaiting a promise-of-identity yields the identity).
      {
        code: `async function Loader(props) { return axios.get(await props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // String concatenation (`+`) remains VALUE-PRESERVING — `ky` bare-client sink.
      {
        code: `function Orders(props) { return ky("/q?u=" + props.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Ternary SELECTED branch remains VALUE-PRESERVING — `got` bare-client sink.
      {
        code: `function Orders(props, cond, x) { return got(cond ? props.userId : x) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Nullish-coalescing remains VALUE-PRESERVING (result IS an operand) — `.put` sink.
      {
        code: `function Orders(props) { return axios.put(props.userId ?? sessionUser()) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-10 (codex): the SOURCE-object detector (`isSourceObjectExpr`) did not
      // propagate through AwaitExpression — only `exprHasTaint` (the VALUE side) did. So an
      // AWAITED source (`(await <source>).userId`, or a `const` alias initialized from an
      // awaited source) was a false NEGATIVE: the member/destructure read past the `await`
      // was never recognized as reading off a source object at all. Fixed by adding
      // `AwaitExpression → .argument` to `isSourceObjectExpr` (mirroring how `exprHasTaint`
      // already treats `await` as value-preserving). A genuinely interprocedural awaited call
      // (`await getProps()`) stays out of scope per the documented function-boundary limit —
      // this uses the router-imported-hook source instead, which resolves without crossing a
      // function boundary. THE MISS, now caught: an awaited router-hook call read directly as
      // a member access.
      {
        code: `import { useParams } from 'next/navigation'
async function Orders() { return api.get((await useParams()).userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Same miss, one level removed: a `const` alias initialized from the awaited source,
      // then destructured/read — proves the await-propagation also reaches through the
      // const-alias source-resolution path (`isSourceDef`'s `isSourceObjectExpr(decl.init)`),
      // not only the inline member-read path above.
      {
        code: `async function Orders({ params }) { const p = await params; return api.get(p.userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-11 (codex): the narrowed sinks still catch every in-scope shape —
      // the sink narrowing must not swallow real data-boundary flows.
      // (1) queryKey element of a data hook's config.
      {
        code: `function Orders(props) { return useQuery({ queryKey: ['x', props.userId], queryFn }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (2) query-string interpolation in a native fetch URL template.
      {
        code: `function Orders(props) { return fetch(\`/api/x?userId=\${props.userId}\`) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (3) `where` object field of a data hook's config.
      {
        code: `function Orders(props) { return useQuery({ where: { userId: props.userId } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-12 (codex): RECEIVER PROVENANCE still catches every genuine HTTP-client
      // shape — the false-positive fix must not swallow real flows.
      // (1) an identifier IMPORTED from a known HTTP-client package, receiver of `.get`.
      {
        code: `import axios from 'axios'
function Orders(props) { return axios.get(\`/u/\${props.userId}\`) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (2) a `const` binding initialized from a recognized client-factory call
      // (`axios.create()`), receiver of `.post` with identity in a `where` field.
      {
        code: `function Orders(props) { const api = axios.create(); return api.post({ where: { userId: props.userId } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (3) the CONVENTIONAL-NAME fallback — `api` with no resolvable non-client init —
      // receiver of `.get` with identity in a `params` field.
      {
        code: `function Orders(props) { return api.get('/x', { params: { userId: props.userId } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },

      // ---- Round-13 (codex): factory-root IMPORT PROVENANCE — a RENAMED axios/ky import
      // used as a client-factory root, with a non-conventional instance name, must still be
      // recognized as an HTTP-client receiver (the literal-name-only check missed this).
      // (1) `import http from 'axios'` (renamed default import), `.create()` factory,
      // non-conventional instance name `transport`.
      {
        code: `import http from 'axios'
function Orders(props) { const transport = http.create(); return transport.get(\`/users/\${props.userId}\`) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // (2) `import k from 'ky'` (renamed default import), `.extend()` factory,
      // non-conventional instance name `c`, identity in a `where` field of a POST body.
      {
        code: `import k from 'ky'
function Orders(props) { const c = k.extend(); return c.post({ where: { userId: props.userId } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
    ],
  })
})

// ---- TypeScript syntactic-wrapper coverage --------------------------------------
// espree (the RuleTester default parser) cannot parse TS `as` / `!` / `satisfies`, so
// these cases run under @typescript-eslint/parser, resolved from the frontend workspace
// (which owns it) via createRequire. If the parser is not installed (e.g. a standalone
// plugin clone where frontend deps are absent), the block SKIPS rather than failing —
// the TS wrapper node types are also proven against the real parser by the reference-app
// lint sweep with a TS canary. Present here it runs (frontend deps are installed).
test('ax/no-caller-identity-from-props — TS wrapper coverage', (t) => {
  let tsParser = null
  try {
    const req = createRequire(new URL('../../../frontend/package.json', import.meta.url))
    tsParser = req('@typescript-eslint/parser')
  } catch {
    t.skip('@typescript-eslint/parser not resolvable from frontend workspace — TS cases skipped')
    return
  }
  const tsTester = new RuleTester({
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2024,
      sourceType: 'module',
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
  })
  tsTester.run('ax/no-caller-identity-from-props (ts)', rule, {
    valid: [
      // A session-derived identity wrapped in a TS cast must NOT false-positive.
      { code: `function Orders() { return api.get(useCallerId() as string) }` },
      { code: `function Orders() { const s = auth(); return api.get(s.userId!) }` },
      // A session identity read through a TS cast in the SOURCE position is not a
      // caller-scope source object — `(session as S).userId` must stay clean.
      { code: `function Orders() { const s = auth(); return api.get((s as S).userId) }` },
    ],
    invalid: [
      // TSAsExpression — `props.userId as string`.
      {
        code: `function Orders(props) { return api.get(props.userId as string) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // TSNonNullExpression — `props.userId!`.
      {
        code: `function Orders(props) { return api.get(props.userId!) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // TSSatisfiesExpression — `props.userId satisfies Id`.
      {
        code: `function Orders(props) { return api.get(props.userId satisfies string) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Combo: TS cast INSIDE a nullish-coalescing combiner.
      {
        code: `function Orders(props) { return api.get((props.userId as string) ?? useCallerId()) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Combo: optional-chained member, TS-cast, buried deep in an object argument.
      {
        code: `function Orders(props) { return api.post({ where: { userId: (props?.userId as string) } }) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Round-3 (self-adversarial): TS cast in the SOURCE-OBJECT position of a member read,
      // resolved through an intermediate const — `const uid = (props as Props).userId`.
      {
        code: `function Orders(props) { const uid = (props as Props).userId; return api.get(uid) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
      // Round-3 (self-adversarial): destructure FROM a TS-cast source object.
      {
        code: `function Page(props) { const { userId } = (props as Props); return api.get(userId) }`,
        errors: [{ messageId: 'callerIdentityFromProps' }],
      },
    ],
  })
})
