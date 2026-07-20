/**
 * ax/no-caller-identity-from-props  (FE-AUTHZ, ERROR)
 *
 * For an authz-relevant data fetch/filter/query call, the caller's OWN identity must
 * come from the caller-id hook (useCallerId() / a session source), never from a value
 * handed in from outside the function — component props, Next.js route `params`,
 * `searchParams`, or a destructured function argument. Mirrors the backend rule
 * `practices/rules/caller-authentication-only-no-userid-param.md` (never accept userId
 * via path/query — derive it from Authentication) applied to the FE call boundary: a
 * component/hook/service function that TRUSTS an incoming userId-shaped value instead
 * of deriving it itself is the same structural IDOR risk, one layer up the call stack.
 *
 * THIS FE LINT IS DEFENSE-IN-DEPTH, NOT THE AUTHORITATIVE CONTROL. The client is
 * untrusted, so the AUTHORITATIVE BFLA/IDOR control is the BACKEND object-level authz
 * check plus its sibling rule `caller-authentication-only-no-userid-param` (identity
 * derived from Authentication server-side, with no caller-supplied parameter to modify).
 * This rule catches the honest-developer FE shapes where a component trusts an incoming
 * identity — at `error` severity — WITHOUT attempting unsound whole-program dataflow in a
 * lint. Anything it declares out of scope is still covered by the authoritative backend
 * control; a documented FE miss is never an authz hole on its own.
 *
 * SOUNDNESS BOUNDARY — PROVABLE IMMUTABILITY + PROVENANCE. A heuristic lint cannot soundly
 * do flow-sensitive taint through a binding whose value can CHANGE (the value at a use site
 * depends on statement order / reassignments / in-place mutations — that needs full dataflow
 * analysis, disproportionate here). So a binding's static value is TRUSTED as a taint source
 * ONLY when the binding is PROVABLY IMMUTABLE — decided purely from ESLint scope references
 * (`Variable.references` / `.isWrite()`), NOT flow analysis. The sound, false-positive-free
 * in-scope surface is exactly that flow-safe one:
 *   1. PROVENANCE-confirmed PARAMETER sources — a function parameter named
 *      props/params/searchParams (bare or destructured), or a function's OWN destructured
 *      identity param `{ userId }` — but ONLY while it is NEVER REASSIGNED. A reassigned
 *      parameter (`function F(props){ props = { userId: useCallerId() }; … }` or
 *      `function F({ userId }){ userId = auth(); … }`) holds a value the lint cannot pin
 *      flow-insensitively, so it is NOT a trusted source (round-6 fix).
 *   2. PROVABLY-IMMUTABLE variable resolution — a `const` binding has exactly one definitive
 *      initializer and can never be REASSIGNED, so alias / projection / resolved-init taint
 *      through it is flow-INSENSITIVE-SAFE *for a scalar* value. For an OBJECT/ARRAY value,
 *      `const` freezes the BINDING but NOT the object: a static-slot projection / whole-object
 *      resolution is trusted ONLY when the object binding is also never PROPERTY-MUTATED
 *      (no `q.x = …` / `q['x'] = …` / `delete q.x` / `q.x++` / nested `q.a.b = …` /
 *      `Object.assign(q, …)` / a DESTRUCTURING-ASSIGNMENT target `({ x: q.x } = …)` /
 *      `[q.x] = …`, including nested/computed-key/defaulted pattern targets — round-7).
 *      A mutated const object is treated conservatively (round-6 fix; round-7 extends the
 *      mutation vectors to destructuring-assignment targets).
 *   3. ROUTER-IMPORTED source hooks — `useParams()` / `useSearchParams()` count as a
 *      caller-supplied routing source ONLY when the identifier is IMPORTED from a known
 *      router/navigation package (react-router, react-router-dom, next/navigation,
 *      next/router). Detection is by IMPORT PROVENANCE, never by spelling: a locally
 *      defined `function useParams(){…}` (or a same-named import from another package) is
 *      NOT a source.
 *
 * OUT OF SCOPE (documented heuristic limits, not silent misses — the authoritative
 * backend control still applies to every one of them):
 *   - MUTABLE `let`/`var` bindings are NOT taint-tracked AT ALL. Their value at a use site
 *     is flow-sensitive: `let uid = props.userId; uid = auth(); use(uid)` is SAFE (tainted
 *     init overwritten before use) while `let uid = auth(); use(uid); uid = props.userId`
 *     is NOT (write after the use) — a flow-INSENSITIVE lint cannot separate the two without
 *     a false positive in one direction. So neither a `let uid = props.userId` initializer
 *     NOR a reaching `uid = props.userId` write taints the binding, and a `let p = props`
 *     alias is not a source object. Mutable-variable flows are the principled boundary.
 *   - A REASSIGNED parameter (`props = …` / a destructured `userId = …` / a destructuring
 *     reassignment `({ x } = …)` / `[props] = …`) or a PROPERTY-MUTATED const object
 *     (`q.userId = …` / `delete q.userId` / `q.userId++` / nested `q.a.b = …` /
 *     `Object.assign(q, …)` / a destructuring-assignment target `({ x: q.x } = …)` /
 *     `[q.x] = …`) is treated conservatively: the value at the use site is no longer the
 *     caller-supplied one, so the binding is dropped as a source (round-6; round-7 adds
 *     destructuring-assignment targets to both vectors). A resulting miss is a documented
 *     false-NEGATIVE, never a false positive.
 *   - MUTATION THROUGH A SEPARATE ALIAS (`const r = q; r.userId = …; use(q.userId)`) or
 *     inside an arbitrary called helper (`mutate(q); use(q.userId)`) is NOT detected — that
 *     needs alias / interprocedural analysis. `bindingHasPropertyMutation` inspects only the
 *     binding's OWN references (its own member-chain writes + `Object.assign` hand-off).
 *   - INTERPROCEDURAL / helper-indirection (`function readId(p){return p.userId};
 *     const uid = readId(props)`) — crossing a function boundary needs interprocedural
 *     analysis. `exprHasTaint` never descends into a function body and never scans an
 *     arbitrary call's argument list; identity usage inside a callback body
 *     (`items.filter(i => i.ownerId === userId)`) is likewise not scanned.
 *   - SPREAD-into-object projection (`const q = { ...tainted }; q.userId`), a
 *     `let`-reassigned object, or an interprocedurally-built object — static projection is
 *     bounded to a DIRECT property of a locally-declared CONST object literal.
 *   - NON-ROUTER-imported (or locally-defined) hooks named useParams/useSearchParams; a
 *     dynamically-computed member key (`props[someVar]`); a dynamically-selected sink
 *     (`(cond ? api.get : noop)(props.userId)`).
 *   - POSITIONAL identity into an arbitrary/LOCAL function (`getAvatarColor(userId)`,
 *     `formatName(userId)`, a custom `fetchOrders(userId)` data-wrapper) — the round-11
 *     sink narrowing; see the SINK MODEL / DROPPED sections below. A local data-wrapper
 *     is interprocedural; the authoritative control is the backend authz + the BE rule
 *     caller-authentication-only-no-userid-param.
 *
 * IN-SCOPE MECHANICS (all flow-safe: parameter / const / router-import).
 * Taint is resolved PER LEXICAL BINDING via ESLint scope analysis (not a file-global name
 * set), so a SAFE `const userId = useCallerId()` in one function is never tainted because a
 * sibling/outer function bound a props-derived `userId`, and an inner shadowing binding
 * wins over an outer one of the same name.
 *
 * SOURCE OBJECTS ARE DETECTED BY PROVENANCE, NOT BY NAME. An identifier is a
 * caller-supplied SOURCE OBJECT (props/params/searchParams) only when its BINDING is:
 *   (a) a FUNCTION PARAMETER named props/params/searchParams — bare (`function F(props)`)
 *       or a destructured binding of that name (`function Page({ params, searchParams })`);
 *   (b) the RESULT of a ROUTER-IMPORTED source hook — `const params = useParams()` /
 *       `const searchParams = useSearchParams()` where the hook is imported from a router
 *       package (Next.js App Router / React Router route inputs are caller-supplied);
 *   (c) a `const x = <a source>` ALIAS chain that resolves to (a)/(b) (`const p = props`).
 * A LOCAL binding that merely reuses the name is NOT a source: a `const props = auth()` /
 * `const params = parseThing()` init is not a source shape, and a `let p = props` alias is
 * MUTABLE (out of scope). `isSourceObjectExpr` resolves the binding's def kind
 * (Parameter / router-imported-hook init / const-alias-to-a-source) rather than trusting
 * the name — closing the codex round-4 (name-based) and round-5 (mutable-alias / local-hook)
 * false positives. A source PARAMETER additionally must be NEVER REASSIGNED
 * (`isReassigned`) — closing the codex round-6 parameter-reassignment false positive.
 *
 * SINGLE SOURCE OF TRUTH FOR TAINT. There is exactly ONE recursive taint decision,
 * `exprHasTaint`, and every entry point routes through it identically:
 *   - a DIRECT call argument (and every value reachable inside it), and
 *   - an IDENTIFIER argument that resolves to a `const x = <init>` binding — its taint is
 *     `exprHasTaint(<init>)`, i.e. resolving a CONST identifier to its initializer is taint-
 *     equivalent to writing that initializer expression inline. So `const uid =
 *     props?.userId`, `const uid = 'x' + props.userId`, `const uid = cond ? props.userId
 *     : x`, a chained `const b = a` (a itself a tainted const), and a `const q = { where: {
 *     userId: searchParams?.get('userId') } }` object initializer are all flagged exactly as
 *     if inlined. (A `let`/`var` binding is NOT resolved — see the immutability boundary.
 *     A `const` whose value is an OBJECT/ARRAY that is later PROPERTY-MUTATED is likewise not
 *     resolved — `bindingHasPropertyMutation` drops it (round-6).)
 *   - a STATIC CONTAINER PROJECTION — `const q = { userId: props.userId }; q.userId` — a
 *     member read of a locally-declared `const` object literal whose that-property value
 *     is tainted is itself tainted (the projection reads a tainted slot) — but ONLY when the
 *     object binding is never PROPERTY-MUTATED. `const q = { userId: props.userId };
 *     q.userId = useCallerId(); q.userId` overwrites the slot to safe, so the projection is
 *     dropped (round-6 const-object-property-mutation false positive).
 *
 * Leaf identity sources (where `exprHasTaint` bottoms out):
 *  - a function's OWN parameter destructuring `{ userId }` (any function, any param
 *    position — exactly "the caller decided the identity, not this function");
 *  - destructuring FROM a SOURCE OBJECT (`const { userId } = props`, and — since the
 *    source position follows the same flow model — `const { userId } = props ?? {}`,
 *    `const { userId } = (props as P)`);
 *  - a member read on a SOURCE OBJECT (`const uid = props.userId`, `params['userId']`,
 *    `(props ?? {}).userId`, `(props as P).userId`) or `searchParams.get('userId')`.
 *  - A destructuring RENAME (`const { userId: uid } = props`) does NOT bypass the rule —
 *    taint follows the extracted KEY, not the local binding name.
 *  - Detected identity KEYS: userId, currentUserId, actorId, memberId.
 *  - SOURCE OBJECTS are the PROVENANCE-resolved (a)/(b)/(c) above — NOT bare names. The
 *    source position is itself resolved through transparent wrappers
 *    (optional-chain/parens/TS casts), value-combining operators (`??`/`||`/`&&`/`?:`/
 *    comma), and `await` (round-10 — `(await useParams()).userId` / an awaited
 *    source-alias destructure) via `isSourceObjectExpr`, so a source object reached
 *    through those still counts.
 *
 * SINK MODEL (round-11 — HIGH-CONFIDENCE, FP-SAFE data boundaries ONLY). A tainted value
 * is reported ONLY when it flows into one of these UNAMBIGUOUS data-boundary sinks:
 *   1. DATA-FETCHING HOOKS — `useSWR` family (`useSWR`/`useSWRInfinite`/`useSWRMutation`),
 *      `useQuery`, `useMutation`, `useInfiniteQuery`, `useSuspenseQuery`. Identity anywhere
 *      in the hook's arguments — the config's `queryKey`/`queryFn`/`url`/`params`/`body`/
 *      `variables`/`where` fields, a key template, a direct argument — is data-selecting.
 *   2. HTTP BOUNDARY CALLS — native `fetch(...)`; the bare callable HTTP clients
 *      `axios(...)` / `ky(...)` / `got(...)`; and HTTP-client METHOD member calls
 *      `.get`/`.post`/`.put`/`.patch`/`.delete` (`api.get(...)`, `axios.post(...)`) — BUT
 *      ONLY when the RECEIVER is a PROVEN HTTP-CLIENT object (round-12 — see RECEIVER
 *      PROVENANCE below). The verb-member SHAPE alone is not enough: `map.get(...)`,
 *      `cache.get(...)`, `set.delete(...)` are ordinary collection calls, not sinks.
 *   3. Within those sinks, an identity in a DATA-SELECTING POSITION is caught wherever
 *      the taint recursion reaches it: a query-string interpolation in a URL string/
 *      template (`` `…?userId=${props.userId}` ``, `'?userId=' + props.userId`), a
 *      `where`/`params`/`filter`/`searchParams` object field bound to the identity, a
 *      `queryKey` element, a direct/spread argument, etc. (the taint machinery below).
 *
 * RECEIVER PROVENANCE (round-12 — closes a realistic false positive introduced by the
 * round-11 sink narrowing). A verb-member call (`.get`/`.post`/`.put`/`.patch`/`.delete`)
 * counts as an HTTP sink ONLY when its receiver is a PROVEN HTTP-CLIENT object — never by
 * verb-name alone, mirroring the import-provenance discipline already used for router
 * hooks (`isRouterImportedHook`). The former shape flagged `map.get(props.userId)`,
 * `cache.get(props.userId)`, and `set.delete(props.userId)` — normal Map/cache/Set
 * operations, not HTTP calls. `isHttpClientReceiver` now requires ONE of:
 *   1. an identifier IMPORTED from a known HTTP-client package (`axios`, `ky`, `got`,
 *      `ofetch`, `redaxios`) — resolved via scope analysis exactly like a router-imported
 *      hook, so a RENAMED import (`import myHttp from 'axios'`) still counts;
 *   2. a `const` binding initialized from a recognized client-FACTORY call — `axios.create()`
 *      / `ky.create()`/`.extend()` / `got.extend()` / `ofetch.create()` / `new XxxApi()` /
 *      `new XxxClient()` — where the factory ROOT (`axios`/`ky`/…) is recognized by literal
 *      name OR by import provenance, so a RENAMED root import (`import http from 'axios';
 *      const transport = http.create()`) still counts (round-13 fix — closes the miss where
 *      only the literal package name was checked);
 *   3. a CONVENTIONAL client identifier name — `api`, `http`, `client`, `apiClient`,
 *      `httpClient`, `request` (and the bare well-known package-root names themselves,
 *      `axios`/`ky`/`got`/`ofetch`/`redaxios`, used WITHOUT an import in the same file) —
 *      a DOCUMENTED HEURISTIC FALLBACK, used ONLY when the binding is not PROVABLY a
 *      non-client value: a local provably resolving to `new Map()`/`new Set()`/`new
 *      WeakMap()`/`new WeakSet()`/an object literal/an array literal is NEVER treated as
 *      an HTTP-client receiver, even if its name matches.
 * Anything else — `map`, `cache`, `set`, `store`, an arbitrary local, `this.x` — is NOT an
 * HTTP-client receiver, and the authoritative backend authz control still covers it.
 *
 * DROPPED (round-11 — closes the codex round-11 realistic false positive): the former
 * DATA-VERB-NAME heuristic — any callee (or tagged-template tag) whose NAME merely starts
 * with fetch/get/load/query/find/list/search/filter/where — flagged identity passed
 * POSITIONALLY to an arbitrary/LOCAL function: `getAvatarColor(userId)` (a pure
 * presentation helper), `formatName(userId)`, a custom `fetchOrders(userId)` wrapper. A
 * name prefix cannot distinguish a presentation helper from a data boundary, and a LOCAL
 * data-wrapper is an INTERPROCEDURAL surface (whether its body reaches fetch/axios is a
 * function-boundary question this lint documents as out of scope). These shapes are now
 * DOCUMENTED OUT-OF-SCOPE — the authoritative BFLA control remains the BACKEND authz +
 * the sibling BE rule caller-authentication-only-no-userid-param, exactly as for every
 * other documented limit above. Within an in-scope sink, the tainted identity is caught
 * whether it is:
 *   · a tainted-binding identifier, or a DIRECT member read (`props.userId`,
 *     `params['userId']`, `searchParams.get('userId')`) with no intermediate `const`,
 *   · used as a direct argument (or a spread argument `fetch(...[props.userId])`),
 *   · interpolated into a template-literal argument,
 *   · a property value of an object argument — at ANY nesting depth, through
 *     array-literal elements, nested template literals, AND object/array SPREADS
 *     (`{ ...{ userId: props.userId } }`, `[...[params.userId]]`) (covers the query-config
 *     shape `useQuery({ where: { OR: [{ userId }] } })` and SWR config `{ url: `…${id}` }`).
 *   · buried inside a transparent WRAPPER or COMBINING expression that hands the same
 *     caller-supplied value to the call: optional chaining (`props?.userId`,
 *     `searchParams?.get('userId')` → `ChainExpression`), string concatenation ONLY
 *     (`'/x?userId=' + props.userId` → `BinaryExpression` with `operator === '+'`;
 *     round-8: every OTHER binary operator — equality/relational
 *     `=== !== == != < > <= >= instanceof in`, arithmetic `- * / % **`, bitwise
 *     `& | ^ << >> >>>` — reduces to a boolean/number/NaN that cannot itself be the
 *     identity, so e.g. `id === props.userId` does NOT taint `id`), `??`/`||`/`&&`
 *     (`props.userId ?? useCallerId()` → `LogicalExpression`, both operands — a
 *     Logical operator's result IS one of its operands, unlike Binary/Conditional),
 *     a ternary's SELECTED VALUE ONLY — `.consequent`/`.alternate`, never `.test`
 *     (`cond ? props.userId : x` → `ConditionalExpression`; round-8: the test is a
 *     boolean condition and is never itself the resulting value, so
 *     `cond === props.userId ? a : b` does not taint from the test), a comma/SEQUENCE's
 *     LAST operand ONLY (`(sideEffect(), props.userId)` → `SequenceExpression`; round-9:
 *     the sequence's value is its last element, so earlier evaluated-and-discarded operands
 *     like `(props.userId, safeId)` do NOT taint), an assignment value (`id = props.userId`,
 *     `id += props.userId` → `AssignmentExpression`, right-hand side only), an `await`
 *     (`AwaitExpression` — awaiting a promise-of-identity yields the identity), and the TS
 *     syntactic wrappers `as` / `!` / `<T>` / `satisfies`
 *     (`(props.userId as string)`, `props.userId!`). `exprHasTaint` recurses through
 *     EVERY such VALUE-PRESERVING node type (subject to the value-deriving exclusions —
 *     Binary non-`+` operator, Conditional `.test`, and ALL of UnaryExpression — below),
 *     so a tainted identity anywhere inside a data-combining expression that can actually
 *     carry it is detected.
 *
 * VALUE-PRESERVING vs VALUE-DERIVING NODE CLASSIFICATION (the single design invariant of
 * `exprHasTaint`). A node PROPAGATES taint iff the caller-supplied identity VALUE can flow
 * OUT of it unchanged; a node that computes a NEW value (boolean/number/string/undefined)
 * from the identity does NOT. Every explicitly-handled node is classified:
 *   VALUE-PRESERVING (recurse):
 *     · leaves — `Identifier`, `MemberExpression`, `CallExpression` (source shapes)
 *     · literal containers — `TemplateLiteral`, `ObjectExpression`, `Property`,
 *       `ArrayExpression`, `SpreadElement`
 *     · transparent wrappers — `ChainExpression`, `ParenthesizedExpression`, `TSAsExpression`,
 *       `TSSatisfiesExpression`, `TSNonNullExpression`, `TSTypeAssertion`,
 *       `TSInstantiationExpression` → `.expression`
 *     · `AwaitExpression` → `.argument`
 *     · `LogicalExpression` (`??`/`||`/`&&`) → `.left` + `.right` (result IS an operand)
 *     · `ConditionalExpression` → `.consequent` + `.alternate` ONLY (never `.test`)
 *     · `BinaryExpression` → recurse ONLY when `operator === '+'` (string concat)
 *     · `SequenceExpression` → ONLY the LAST `.expressions[]` element
 *     · `AssignmentExpression` → `.right`
 *   VALUE-DERIVING (do NOT recurse — return false):
 *     · `UnaryExpression` — ALL operators (`! typeof void ~ + - delete`): boolean/string/
 *       number/undefined (round-9 fix — no longer shares AwaitExpression's branch)
 *     · `BinaryExpression` with a non-`+` operator (round-8)
 *     · `ConditionalExpression.test` (round-8)
 *     · `UpdateExpression` (`++`/`--`) — number
 *     · default (any unlisted node, incl. Function/Arrow bodies) → false (conservative)
 *
 * Spec: specs/react-practices-l0.yaml#REACT-PRACTICES-SECURITY-AUTHZ-001.
 * Backend analog (AUTHORITATIVE control): practices/rules/caller-authentication-only-no-userid-param.md.
 */

const IDENTITY_NAMES = new Set(['userId', 'currentUserId', 'actorId', 'memberId'])
// Canonical caller-supplied SOURCE-OBJECT names a component/route receives. Matched by
// PROVENANCE (binding def kind), never by bare name — see isSourceObjectExpr in create().
const SOURCE_OBJECT_NAMES = new Set(['props', 'params', 'searchParams'])
// Hooks whose RESULT is a caller-supplied routing source (Next.js App Router / React
// Router). A source ONLY when IMPORTED from a router package (see ROUTER_SOURCE_PACKAGES /
// isRouterImportedHook) — never by spelling. `const params = useParams()`.
const SOURCE_HOOK_NAMES = new Set(['useParams', 'useSearchParams'])
// The router/navigation packages whose useParams/useSearchParams exports are caller-supplied
// route inputs. A same-named identifier from anywhere else (local definition, unrelated
// import) is NOT a source — this closes the codex round-5 spelling false positive.
const ROUTER_SOURCE_PACKAGES = new Set([
  'react-router',
  'react-router-dom',
  'next/navigation',
  'next/router',
])
// ---- SINK SET (round-11 — high-confidence, FP-safe data boundaries ONLY) ---------
// 1. Data-fetching HOOKS — identity anywhere in their arguments (queryKey / config
//    object / url / params / body / variables / where) is data-selecting.
const KNOWN_DATA_HOOKS = new Set([
  'useSWR', 'useSWRInfinite', 'useSWRMutation',
  'useQuery', 'useMutation', 'useInfiniteQuery', 'useSuspenseQuery',
])
// 2a. Bare HTTP-boundary callees — native fetch(...) and the callable HTTP clients
//     axios(...) / ky(...) / got(...).
const HTTP_CLIENT_CALLEES = new Set(['fetch', 'axios', 'ky', 'got'])
// 2b. HTTP-client METHOD member calls — api.get(...) / axios.post(...) / http.delete(...).
//     The member-call shape ALONE is NOT enough (round-12 fix — see the RECEIVER
//     PROVENANCE section below): the receiver must additionally be a PROVEN HTTP-client
//     object, or an ordinary `map.get(...)`/`set.delete(...)` collection call false-flags.
const HTTP_METHOD_NAMES = new Set(['get', 'post', 'put', 'patch', 'delete'])
// ---- HTTP-CLIENT RECEIVER PROVENANCE (round-12) ----------------------------------
// A verb-member call (`.get`/`.post`/`.put`/`.patch`/`.delete`) is a data-boundary sink
// ONLY when its receiver is a PROVEN http-client object — never by verb-name alone. See
// `isHttpClientReceiver` in create() for the full decision (mirrors `isRouterImportedHook`'s
// import-provenance pattern).
// 1. Well-known HTTP-client package root names — trusted whether reached via a bare
//    identifier (`axios.get(...)`) or via import-provenance (a renamed import,
//    `import myHttp from 'axios'; myHttp.get(...)`).
const HTTP_CLIENT_PACKAGES = new Set(['axios', 'ky', 'got', 'ofetch', 'redaxios'])
// 2. Client-factory METHOD names — `axios.create(...)` / `ky.create(...)`/`.extend(...)` /
//    `got.extend(...)` / `ofetch.create(...)` produce an HTTP-client INSTANCE.
const CLIENT_FACTORY_METHODS = new Set(['create', 'extend'])
// 3. Conventional client identifier names — a DOCUMENTED HEURISTIC FALLBACK, used only
//    when the binding cannot be proven to be a non-client value (see
//    `isProvablyNonClientBinding`). Not provenance-confirmed by itself.
const CONVENTIONAL_CLIENT_NAMES = new Set(['api', 'http', 'client', 'apiClient', 'httpClient', 'request'])

/**
 * Transparent single-child wrappers whose wrapped child IS the same runtime value
 * (optional-chain / parens / TS type-only casts). Peeling them changes nothing at
 * runtime, so taint/source detection must see through them.
 */
const TRANSPARENT_WRAPPERS = new Set([
  'ChainExpression', // props?.userId, searchParams?.get('userId')
  'ParenthesizedExpression', // (props.userId) when the parser preserves parens
  'TSAsExpression', // props.userId as string
  'TSSatisfiesExpression', // props.userId satisfies Id
  'TSNonNullExpression', // props.userId!
  'TSTypeAssertion', // <string>props.userId
  'TSInstantiationExpression', // fn<Id>
])

/** Peel transparent wrappers off `node`, returning the underlying runtime-value node. */
function unwrapTransparent(node) {
  let n = node
  while (n && TRANSPARENT_WRAPPERS.has(n.type)) n = n.expression
  return n
}

/** Non-computed key → its name; computed key → its literal string value, else null. */
function getPropertyKeyName(key, computed) {
  if (!computed) {
    if (key.type === 'Identifier') return key.name
    if (key.type === 'Literal' && typeof key.value === 'string') return key.value
    return null
  }
  return key.type === 'Literal' && typeof key.value === 'string' ? key.value : null
}

/** Human-readable sink name for the report message. */
function calleeName(callee) {
  return callee.type === 'Identifier'
    ? callee.name
    : callee.type === 'MemberExpression' && callee.property.type === 'Identifier'
      ? callee.property.name
      : '<call>'
}

/**
 * Does `node` construct an HTTP-CLIENT INSTANCE — `axios.create(...)` / `ky.create(...)` /
 * `ky.extend(...)` / `got.extend(...)` / `ofetch.create(...)`, or `new XxxApi(...)` /
 * `new XxxClient(...)`? The factory-root object is recognized by EITHER a bare
 * package-root name (`HTTP_CLIENT_PACKAGES`, no scope resolution needed) OR — via the
 * optional `resolveVariable` resolver, mirroring `isRouterImportedHook`'s import-provenance
 * pattern — an identifier whose BINDING is imported from a known HTTP-client package. This
 * closes the round-13 miss where a RENAMED axios import (`import http from 'axios'; const
 * transport = http.create()`) was invisible to the literal-name check alone.
 */
function isHttpClientFactoryCall(node, resolveVariable) {
  const n = unwrapTransparent(node)
  if (!n) return false
  if (n.type === 'NewExpression' && n.callee.type === 'Identifier') {
    return /(Api|Client)$/.test(n.callee.name)
  }
  if (n.type === 'CallExpression') {
    const callee = n.callee
    if (callee.type !== 'MemberExpression' || callee.computed) return false
    if (callee.property.type !== 'Identifier' || !CLIENT_FACTORY_METHODS.has(callee.property.name)) return false
    if (callee.object.type !== 'Identifier') return false
    if (HTTP_CLIENT_PACKAGES.has(callee.object.name)) return true
    return !!resolveVariable && isHttpClientImportBinding(resolveVariable(callee.object))
  }
  return false
}

/**
 * Is `variable` an IMPORT BINDING whose `ImportDeclaration` source is a known HTTP-client
 * package (axios/ky/got/ofetch/redaxios)? Mirrors `isRouterImportedHook`'s import-provenance
 * pattern — this additionally recognizes a RENAMED import (`import myHttp from 'axios'`)
 * that the bare-name check in `isHttpClientReceiver` would miss.
 */
function isHttpClientImportBinding(variable) {
  if (!variable) return false
  return variable.defs.some((def) => {
    if (def.type !== 'ImportBinding') return false
    const decl = def.parent
    if (!decl || decl.type !== 'ImportDeclaration' || !decl.source) return false
    return typeof decl.source.value === 'string' && HTTP_CLIENT_PACKAGES.has(decl.source.value)
  })
}

/**
 * Does `variable`'s binding PROVABLY resolve to a NON-HTTP-CLIENT value — `new Map()`,
 * `new Set()`, `new WeakMap()`/`new WeakSet()`, a bare object literal `{}`, or an array
 * literal `[]`? Gates the CONVENTIONAL-NAME fallback in `isHttpClientReceiver`: a locally
 * named `api`/`client`/... that is provably a Map/Set/object/array must NOT be treated as
 * an HTTP-client receiver just because its name matches (round-12 hardening).
 */
function isProvablyNonClientBinding(variable) {
  if (!variable || variable.defs.length !== 1) return false
  const def = variable.defs[0]
  if (def.type !== 'Variable' || def.node.type !== 'VariableDeclarator') return false
  const init = unwrapTransparent(def.node.init)
  if (!init) return false
  if (init.type === 'ObjectExpression' || init.type === 'ArrayExpression') return true
  return (
    init.type === 'NewExpression' &&
    init.callee.type === 'Identifier' &&
    ['Map', 'Set', 'WeakMap', 'WeakSet'].includes(init.callee.name)
  )
}

/**
 * Is `variable` a `const` binding initialized from a recognized HTTP-client FACTORY call
 * (`const api = axios.create()`, or — via `resolveVariable` — `const transport =
 * http.create()` where `http` is a renamed axios import)? See `isHttpClientFactoryCall`.
 */
function isConstBindingFromFactory(variable, resolveVariable) {
  if (!variable || variable.defs.length !== 1) return false
  const def = variable.defs[0]
  if (def.type !== 'Variable' || def.node.type !== 'VariableDeclarator') return false
  if (!def.node.parent || def.node.parent.kind !== 'const') return false
  return isHttpClientFactoryCall(def.node.init, resolveVariable)
}

/**
 * A destructuring binding identifier (the local name) → the KEY it was extracted under,
 * if it is a DIRECT property of an ObjectPattern (shorthand `{ userId }`, rename
 * `{ userId: uid }`, or default `{ userId = d }`). Returns the pattern + key so callers
 * can additionally check the pattern's owner. Nested patterns return null.
 */
function destructureKeyInfo(idNode) {
  let n = idNode
  if (n.parent && n.parent.type === 'AssignmentPattern' && n.parent.left === n) n = n.parent
  const prop = n.parent
  if (!prop || prop.type !== 'Property' || prop.value !== n) return null
  const pattern = prop.parent
  if (!pattern || pattern.type !== 'ObjectPattern') return null
  const keyName = getPropertyKeyName(prop.key, prop.computed)
  if (keyName === null) return null
  return { keyName, pattern }
}

// ---- PROVABLE-IMMUTABILITY PRIMITIVES (codex round-6) ----------------------------
// A binding's static value can only be TRUSTED as a taint source if the binding is
// PROVABLY IMMUTABLE — never REASSIGNED and (for objects/arrays) never PROPERTY-MUTATED.
// This is decided purely from ESLint scope references (Variable.references / .isWrite()),
// NOT flow analysis. A reassigned/mutated binding is treated CONSERVATIVELY (not a trusted
// source → no flag → a possible, documented false-negative), because a flow-insensitive
// lint cannot know the value at the use site once the binding can change.

/**
 * Pattern-container node types a destructuring-ASSIGNMENT (`(...) = …`, never a declaration)
 * target can be nested inside, en route up to the `AssignmentExpression` it belongs to:
 * `Property` (an ObjectPattern property's value slot — also covers a computed key
 * `{ [k]: q.userId }`, since only the VALUE slot passes), `ObjectPattern`, `ArrayPattern` (an
 * element slot), `AssignmentPattern` (a defaulted target, `{ a: q.userId = d } = safe`), and
 * `RestElement` (`...q.userId`). Walking through these from a candidate target node (an
 * `Identifier` OR a `MemberExpression`) reaches `AssignmentExpression.left` iff `node` is a
 * genuine write target of a destructuring assignment — never its RHS default value or a
 * property KEY, both of which fail the slot check at their level and return `false`
 * immediately (round-7 fix).
 */
function isPatternAssignmentTarget(node) {
  let child = node
  let parent = child.parent
  while (parent) {
    switch (parent.type) {
      case 'Property':
        if (parent.value !== child) return false // excludes the KEY slot
        break
      case 'ObjectPattern':
        if (!parent.properties.includes(child)) return false
        break
      case 'ArrayPattern':
        if (!parent.elements.includes(child)) return false
        break
      case 'AssignmentPattern':
        if (parent.left !== child) return false // excludes the default-VALUE slot
        break
      case 'RestElement':
        if (parent.argument !== child) return false
        break
      case 'AssignmentExpression':
        return parent.left === child
      default:
        return false
    }
    child = parent
    parent = child.parent
  }
  return false
}

/**
 * Is this Variable REASSIGNED anywhere? A write reference that is NOT the binding's own
 * initialization means the value at a use site is flow-sensitive → not provably immutable.
 * (`const`/destructured-const bindings never are; a reassigned parameter or destructured
 * identity param IS — closing the round-6 parameter-reassignment false positive.)
 *
 * ALSO reassigned when the binding's own identifier occurrence sits as an IDENTIFIER TARGET
 * anywhere inside a destructuring-ASSIGNMENT LHS pattern (`({ x } = safe)`, `[props] = safe`,
 * a nested/defaulted/rest target). In practice ESLint scope analysis already surfaces this
 * shape as a write reference (`isWrite() && !init`) — verified directly against this rule's
 * two configured parsers (espree via RuleTester, `@typescript-eslint/parser` via the frontend
 * lint config) — but the structural check is kept as an explicit, parser-independent OR so the
 * verdict does not silently depend on that scope-analysis detail (round-7 hardening).
 */
function isReassigned(variable) {
  if (!variable) return false
  return variable.references.some(
    (r) => (r.isWrite() && !r.init) || isPatternAssignmentTarget(r.identifier)
  )
}

/** Is `member` (a MemberExpression) the TARGET of a write (assign-left / delete / ++/--)? */
function memberIsWriteTarget(member) {
  const p = member.parent
  if (!p) return false
  if (p.type === 'AssignmentExpression' && p.left === member) return true // q.x = / q.x += …
  if (p.type === 'UpdateExpression' && p.argument === member) return true // q.x++ / q.x--
  if (p.type === 'UnaryExpression' && p.operator === 'delete' && p.argument === member) return true // delete q.x
  // `({ userId: q.userId } = safe)` / `[q.userId] = safe` / nested (`{ a: { b: q.userId } } =
  // safe`) / computed-key (`{ [k]: q.userId } = safe`) / defaulted (`{ a: q.userId = d } =
  // safe`) — a DESTRUCTURING-ASSIGNMENT pattern target. ESLint scope analysis treats the
  // member's OBJECT here as a plain READ reference (escope's PatternVisitor hands a
  // MemberExpression target's `.object` to the right-hand-side, never a write — verified
  // directly for both this rule's parsers), so it is NOT surfaced as a write ref. This
  // structural walk recovers it (round-7 fix — closes the codex round-7 false positive where
  // `fetchOrders(q)` stayed flagged after `({ userId: q.userId } = safe)` mutated `q`).
  if (isPatternAssignmentTarget(member)) return true
  return false
}

/**
 * Does the reference `id` (a binding identifier) sit at the ROOT of a member-access chain
 * (`q`, `q.a`, `q.a.b`, `q['x']`, …) whose OUTERMOST access is a write target? Catches a
 * direct property write `q.userId = …`, a computed one `q['userId'] = …`, an update
 * `q.userId++`, a `delete q.userId`, AND a nested-object mutation `q.inner.userId = …`.
 */
function referenceMutatesViaMemberChain(id) {
  let node = id
  let parent = node.parent
  while (parent && parent.type === 'MemberExpression' && parent.object === node) {
    if (memberIsWriteTarget(parent)) return true
    node = parent
    parent = node.parent
  }
  return false
}

/**
 * Is `id` passed as an argument to a KNOWN intrinsic MUTATOR — `Object.assign(q, …)`,
 * `Object.defineProperty(q, …)`, `Object.defineProperties(q, …)`, `Object.setPrototypeOf(q, …)`?
 * These mutate their target in place, so a bound object handed to one is no longer trusted.
 * (Conservative: any argument position counts — over-suppression is a false negative, never
 * a false positive.)
 */
function isKnownMutatorArgument(id) {
  const call = id.parent
  if (!call || call.type !== 'CallExpression' || !call.arguments.includes(id)) return false
  const callee = call.callee
  return (
    callee.type === 'MemberExpression' &&
    !callee.computed &&
    callee.object.type === 'Identifier' &&
    callee.object.name === 'Object' &&
    callee.property.type === 'Identifier' &&
    ['assign', 'defineProperty', 'defineProperties', 'setPrototypeOf'].includes(callee.property.name)
  )
}

/**
 * Is this object/array binding PROPERTY-MUTATED anywhere — a member-chain write
 * (`q.x = …`, `q['x'] = …`, `delete q.x`, `q.x++`, nested `q.a.b = …`), a hand-off to a
 * known intrinsic mutator (`Object.assign(q, …)`), or a DESTRUCTURING-ASSIGNMENT target
 * (`({ x: q.x } = …)`, `[q.x] = …`, nested/computed-key/defaulted — round-7)? If so, the
 * binding's initializer no longer reflects the value at the use site, so its static slot(s)
 * cannot be trusted. A const binding cannot be REASSIGNED, but its object CAN be mutated in
 * place — this closes the round-6 const-object-property-mutation false positive (round-7
 * extends it to the destructuring-assignment-target mutation vector, via
 * `memberIsWriteTarget`'s `isPatternAssignmentTarget` check).
 *
 * DOCUMENTED OUT-OF-SCOPE (needs alias/interprocedural analysis, not attempted): mutation
 * through a SEPARATE alias binding (`const r = q; r.userId = …`) and mutation inside an
 * arbitrary called helper (`mutate(q)`). The authoritative backend authz control covers them.
 */
function bindingHasPropertyMutation(variable) {
  if (!variable) return false
  return variable.references.some((ref) => {
    if (ref.init) return false // the binding's own initializer, not a mutation
    if (ref.isWrite()) return true // a reassignment (defensive — const shouldn't reach here)
    const id = ref.identifier
    return referenceMutatesViaMemberChain(id) || isKnownMutatorArgument(id)
  })
}

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: {
      description:
        "Caller identity (userId/currentUserId/actorId/memberId) fed into a data-boundary sink (a data-fetching hook like useQuery/useSWR, native fetch, or an HTTP client call like api.get/axios.post) must come from the caller-id hook (useCallerId()/session), not from props, params, searchParams, or a destructured function argument.",
      recommended: true,
      url: 'https://github.com/ax-template/practices-react/blob/main/rules/no-caller-identity-from-props.md',
    },
    schema: [],
    messages: {
      callerIdentityFromProps:
        "Authz-relevant call '{{call}}' receives caller identity '{{name}}' from props/params/searchParams/a destructured function argument. Derive the caller from useCallerId()/session inside this function instead — never trust an identity value handed in from outside (CWE-639 / structural IDOR).",
    },
  },

  create(context) {
    const sourceCode = context.sourceCode || context.getSourceCode()
    /** Memoized taint verdict per resolved Variable. */
    const taintCache = new WeakMap()
    /** Memoized source-object verdict per resolved Variable (provenance). */
    const sourceCache = new WeakMap()
    /** Re-entry guard for const object-literal projection recursion. */
    const projectionGuard = new Set()

    /** Resolve an identifier reference node to its declared Variable (scope-aware). */
    function resolveVariable(idNode) {
      let scope = sourceCode.getScope(idNode)
      while (scope) {
        const ref = scope.references.find((r) => r.identifier === idNode)
        if (ref) return ref.resolved
        scope = scope.upper
      }
      return null
    }

    /**
     * Is `idNode` a reference to a source hook (useParams/useSearchParams) IMPORTED from a
     * known router package? Detection is by IMPORT PROVENANCE, not spelling: the binding
     * must be an ImportBinding whose ImportDeclaration source is a router package AND whose
     * IMPORTED name (not the local rename) is a source hook. A locally-defined function or a
     * same-named import from an unrelated package is NOT a source. Closes the round-5
     * spelling false positive (`function useParams(){return auth()}`).
     */
    function isRouterImportedHook(idNode) {
      const variable = resolveVariable(idNode)
      if (!variable) return false
      return variable.defs.some((def) => {
        if (def.type !== 'ImportBinding') return false
        const decl = def.parent
        if (!decl || decl.type !== 'ImportDeclaration' || !decl.source) return false
        if (typeof decl.source.value !== 'string' || !ROUTER_SOURCE_PACKAGES.has(decl.source.value)) {
          return false
        }
        const spec = def.node
        const importedName =
          spec.type === 'ImportSpecifier' && spec.imported && spec.imported.type === 'Identifier'
            ? spec.imported.name
            : null
        return importedName !== null && SOURCE_HOOK_NAMES.has(importedName)
      })
    }

    /**
     * `useParams()` / `useSearchParams()` — a bare source-hook call producing a source
     * object, ONLY when the callee is a router-imported source hook (by provenance).
     */
    function isSourceHookCall(node) {
      return (
        !!node &&
        node.type === 'CallExpression' &&
        node.callee.type === 'Identifier' &&
        isRouterImportedHook(node.callee)
      )
    }

    // ---- HTTP-CLIENT RECEIVER PROVENANCE (round-12) ---------------------------------
    // Closes the codex round-12 realistic false positive: the former `isDataCallee` treated
    // ANY `.get/.post/.put/.patch/.delete` MEMBER call as an HTTP sink regardless of its
    // receiver, so ordinary JS collection calls false-flagged — `map.get(props.userId)`,
    // `cache.get(props.userId)`, `set.delete(props.userId)` are normal frontend constructs,
    // not HTTP boundaries. A verb-member call is now a sink ONLY when the receiver is a
    // PROVEN HTTP-CLIENT object, resolved by provenance exactly like `isRouterImportedHook`:

    /**
     * Is `node` (the object/receiver of a `.get/.post/.put/.patch/.delete` member call) a
     * PROVEN HTTP-CLIENT receiver? Three mechanisms, in order:
     *   1. a `const` binding initialized from a recognized client-FACTORY call
     *      (`const api = axios.create()` / `ky.create()`/`.extend()` / `got.extend()` /
     *      `ofetch.create()` / `new XxxApi()` / `new XxxClient()`);
     *   2. an identifier IMPORTED from a known HTTP-client package — resolved via scope
     *      analysis, mirroring `isRouterImportedHook` (covers a RENAMED import);
     *   3. a NAME-based match — a well-known package-root name (axios/ky/got/ofetch/
     *      redaxios) or a conventional client identifier (api/http/client/apiClient/
     *      httpClient/request) — a DOCUMENTED HEURISTIC FALLBACK, used ONLY when the
     *      binding is not PROVABLY a non-client value (Map/Set/object/array literal).
     * Anything else — `map`, `cache`, `set`, `store`, an arbitrary local, `this.x` — is
     * NOT an HTTP-client receiver.
     */
    function isHttpClientReceiver(node) {
      const n = unwrapTransparent(node)
      if (!n || n.type !== 'Identifier') return false
      const variable = resolveVariable(n)
      if (isConstBindingFromFactory(variable, resolveVariable)) return true
      if (isHttpClientImportBinding(variable)) return true
      if (
        (HTTP_CLIENT_PACKAGES.has(n.name) || CONVENTIONAL_CLIENT_NAMES.has(n.name)) &&
        !isProvablyNonClientBinding(variable)
      ) {
        return true
      }
      return false
    }

    /**
     * Is `callee` an in-scope authz DATA-BOUNDARY sink? Exactly the round-11 narrowed set,
     * with round-12's receiver-provenance requirement on the member-call branch: a known
     * data-fetching hook, a bare HTTP-boundary callee (fetch/axios/ky/got), or an HTTP-verb
     * MEMBER call (.get/.post/.put/.patch/.delete) whose RECEIVER is a proven HTTP-client
     * object (`isHttpClientReceiver`). An arbitrary identifier whose name merely LOOKS
     * data-ish (getAvatarColor, formatName, a local fetchOrders wrapper) is NOT a sink —
     * documented out of scope, covered by the authoritative backend authz. Neither is an
     * ordinary collection/cache/store member call (`map.get`, `cache.get`, `set.delete`) —
     * documented out of scope by the SAME reasoning (no receiver provenance), closing the
     * round-12 false positive.
     */
    function isDataCallee(callee) {
      if (callee.type === 'Identifier') {
        return KNOWN_DATA_HOOKS.has(callee.name) || HTTP_CLIENT_CALLEES.has(callee.name)
      }
      if (callee.type === 'MemberExpression' && !callee.computed && callee.property.type === 'Identifier') {
        return HTTP_METHOD_NAMES.has(callee.property.name) && isHttpClientReceiver(callee.object)
      }
      return false
    }

    // ---- PROVENANCE-BASED SOURCE-OBJECT DETECTION -----------------------------------
    // A binding is a caller-supplied source object only when its DEF makes it one; a
    // local `const props = auth()` (init not a source shape) or a `let p = props` (mutable,
    // out of scope) is NOT. This resolves the codex round-4 (name-based) + round-5
    // (mutable-alias / local-hook) false positives.

    /** Does this binding's declaration make it a caller-supplied SOURCE OBJECT? */
    function isSourceDef(def, variable) {
      // (a) a function PARAMETER named props/params/searchParams — bare or a destructured
      //     binding identifier of that name. That is the object the caller handed in — but
      //     ONLY while it is PROVABLY IMMUTABLE. A REASSIGNED parameter
      //     (`function F(props){ props = { userId: useCallerId() }; … }`) no longer holds
      //     the caller-supplied object at the use site, so it is NOT a trusted source
      //     (round-6 parameter-reassignment false positive).
      if (def.type === 'Parameter') {
        return (
          def.name.type === 'Identifier' &&
          SOURCE_OBJECT_NAMES.has(def.name.name) &&
          !isReassigned(variable)
        )
      }
      // (b)/(c) a CONST alias/hook-result that RESOLVES to a source object — an alias chain
      //     to a source param, or a router-imported source-hook result. Only `const` is
      //     taint-resolved: a `let`/`var` binding is MUTABLE and out of scope (see the
      //     immutability boundary), so its value at the use site is flow-sensitive.
      if (
        def.type === 'Variable' &&
        def.node.type === 'VariableDeclarator' &&
        def.node.parent &&
        def.node.parent.kind === 'const'
      ) {
        const decl = def.node
        if (decl.init && decl.id.type === 'Identifier' && def.name === decl.id) {
          return isSourceObjectExpr(decl.init)
        }
      }
      return false
    }

    function isSourceObjectBinding(variable) {
      if (!variable) return false
      if (sourceCache.has(variable)) return sourceCache.get(variable)
      // Guard against cyclic memo re-entry (e.g. `const a = b; const b = a`).
      sourceCache.set(variable, false)
      const verdict = variable.defs.some((def) => isSourceDef(def, variable))
      sourceCache.set(variable, verdict)
      return verdict
    }

    /**
     * Does `node` denote a caller-supplied SOURCE OBJECT — reached through transparent
     * wrappers, value-combining operators (`??`/`||`/`&&`/`?:`/comma), and `await`? At an
     * Identifier leaf the decision is by PROVENANCE (the binding's def kind), not by name; a
     * bare router-imported `useParams()`/`useSearchParams()` call is a source too. Mirrors the
     * flow model `exprHasTaint` uses for identity VALUES, so the OBJECT position of a member
     * read (`(props ?? {}).userId`) and the INIT of a destructure (`const { userId } = props
     * ?? {}`) are followed identically. `await` propagates like everywhere else in this rule
     * (round-10 — `(await useParams()).userId` resolves to the source once the promise
     * settles). Recursion stops at a function boundary as everywhere.
     */
    function isSourceObjectExpr(node) {
      const n = unwrapTransparent(node)
      if (!n) return false
      switch (n.type) {
        case 'Identifier':
          return isSourceObjectBinding(resolveVariable(n))
        case 'CallExpression': // useParams() / useSearchParams() used inline (router-imported)
          return isSourceHookCall(n)
        case 'LogicalExpression': // (props ?? {}) , (a || props)
          return isSourceObjectExpr(n.left) || isSourceObjectExpr(n.right)
        case 'ConditionalExpression': // (cond ? props : other) — result is a branch, not the test
          return isSourceObjectExpr(n.consequent) || isSourceObjectExpr(n.alternate)
        case 'SequenceExpression': // (sideEffect(), props) — value is the last operand
          return isSourceObjectExpr(n.expressions[n.expressions.length - 1])
        case 'AwaitExpression': // (await useParams()) — an awaited source resolves to the
          // source object itself once settled. Mirrors exprHasTaint's AwaitExpression
          // handling for VALUES (round-9); this closes the round-10 miss where the
          // SOURCE-object side of the same node type was not propagated (`(await
          // useParams()).userId` / an awaited source-alias destructure stayed undetected).
          return isSourceObjectExpr(n.argument)
        default:
          return false
      }
    }

    /** `props.userId` / `params['userId']` / `(props ?? {}).userId` — object is a source object. */
    function isIdentitySourceMember(node) {
      if (!node || node.type !== 'MemberExpression') return false
      if (!isSourceObjectExpr(node.object)) return false
      const propName = getPropertyKeyName(node.property, node.computed)
      return propName !== null && IDENTITY_NAMES.has(propName)
    }

    /** `searchParams.get('userId')` — the object must be a source object (by provenance). */
    function isSearchParamsGetCall(node) {
      if (!node || node.type !== 'CallExpression') return false
      const callee = node.callee
      if (callee.type !== 'MemberExpression' || callee.computed) return false
      if (callee.property.type !== 'Identifier' || callee.property.name !== 'get') return false
      if (!isSourceObjectExpr(callee.object)) return false
      const arg = node.arguments[0]
      return !!arg && arg.type === 'Literal' && typeof arg.value === 'string' && IDENTITY_NAMES.has(arg.value)
    }

    /**
     * `const q = { userId: <tainted> }; q.userId` — a member read of a locally-declared
     * `const` OBJECT LITERAL whose that-property value is tainted. Bounded to a direct,
     * non-computed property of the literal; a value reached only through a SPREAD, a
     * `let`-reassigned object, or an interprocedurally-built object is the documented
     * heuristic limit (function-boundary / immutability line).
     */
    function isTaintedObjectProjection(node) {
      if (node.type !== 'MemberExpression' || node.object.type !== 'Identifier') return false
      const key = getPropertyKeyName(node.property, node.computed)
      if (key === null) return false
      const objExpr = constObjectInit(resolveVariable(node.object))
      if (!objExpr || projectionGuard.has(objExpr)) return false
      projectionGuard.add(objExpr) // re-entry guard against `const q = { userId: q.userId }`
      try {
        return objExpr.properties.some(
          (p) =>
            p.type === 'Property' &&
            !p.computed &&
            getPropertyKeyName(p.key, p.computed) === key &&
            exprHasTaint(p.value)
        )
      } finally {
        projectionGuard.delete(objExpr)
      }
    }

    /**
     * A single `const x = { ... }` declarator's ObjectExpression init, else null — but ONLY
     * when the binding is PROVABLY IMMUTABLE. A const object whose binding is
     * property-mutated (`const q = { userId: props.userId }; q.userId = useCallerId(); …`)
     * cannot be trusted for static-slot projection: its initializer no longer reflects the
     * value at the use site. Backing off → conservative (no flag). Closes the round-6
     * const-object-property-mutation false positive.
     */
    function constObjectInit(variable) {
      if (!variable || variable.defs.length !== 1) return null
      const def = variable.defs[0]
      if (def.type !== 'Variable' || def.node.type !== 'VariableDeclarator') return null
      if (def.node.id.type !== 'Identifier' || def.name !== def.node.id) return null
      if (!def.node.parent || def.node.parent.kind !== 'const') return null
      if (bindingHasPropertyMutation(variable)) return null // mutated → not provably immutable
      const init = unwrapTransparent(def.node.init)
      return init && init.type === 'ObjectExpression' ? init : null
    }

    /** Does this binding's declaration make it a tainted (caller-supplied) identity? */
    function isTaintedDef(def, variable) {
      // (a) function's OWN parameter destructuring: `function F({ userId }) {}`.
      // Only a DIRECT property of a top-level ObjectPattern param counts. Parameters are
      // caller-decided by nature, so a param source is in scope (not subject to the
      // const-only rule, which governs local VARIABLE bindings) — but ONLY while PROVABLY
      // IMMUTABLE. A destructured identity param that is REASSIGNED
      // (`function F({ userId }){ userId = auth(); … }`) no longer carries the caller-supplied
      // value at the use site, so it is not a trusted source (round-6 immutability boundary).
      if (def.type === 'Parameter') {
        const info = destructureKeyInfo(def.name)
        return (
          !!info &&
          IDENTITY_NAMES.has(info.keyName) &&
          Array.isArray(def.node.params) &&
          def.node.params.includes(info.pattern) &&
          !isReassigned(variable)
        )
      }
      // Local VARIABLE bindings: only `const` is taint-resolved. A `let`/`var` binding is
      // MUTABLE, so the value at a use site is flow-sensitive (statement order / reassignment)
      // — a heuristic lint deliberately does NOT model that, so mutable variables are OUT OF
      // SCOPE. Immutability + provenance is the soundness boundary (round-5 fix).
      if (
        def.type === 'Variable' &&
        def.node.type === 'VariableDeclarator' &&
        def.node.parent &&
        def.node.parent.kind === 'const'
      ) {
        const decl = def.node
        if (!decl.init) return false
        // (c) `const uid = <init>` — taint is the SAME exhaustive recursion used for a
        // direct call argument, so resolving the identifier is taint-equivalent to writing
        // `<init>` inline (optional-chain / concat / ternary / TS-wrapped / spread /
        // deep-object / chained-const initializers all included). Exactly one taint
        // decision, `exprHasTaint`, for both entry points.
        //
        // A `const` binding cannot be REASSIGNED, but if its value is a MUTABLE container
        // (object/array literal) that is later PROPERTY-MUTATED (`const q = { userId:
        // props.userId }; q.userId = useCallerId(); fetchOrders(q)`), the initializer no
        // longer reflects the value at the use site — back off (round-6 immutability
        // boundary). Scalar initializers (a `string`/`number` value) are immutable by
        // nature, so they are never gated.
        if (decl.id.type === 'Identifier' && def.name === decl.id) {
          const init = unwrapTransparent(decl.init)
          if (
            init &&
            (init.type === 'ObjectExpression' || init.type === 'ArrayExpression') &&
            bindingHasPropertyMutation(variable)
          ) {
            return false
          }
          return exprHasTaint(decl.init)
        }
        // (b) `const { userId } = <source>` — direct identity-key property of the pattern,
        // where the destructured init is a source object (bare, wrapped, or combined).
        if (decl.id.type === 'ObjectPattern' && isSourceObjectExpr(decl.init)) {
          const info = destructureKeyInfo(def.name)
          return !!info && info.pattern === decl.id && IDENTITY_NAMES.has(info.keyName)
        }
      }
      return false
    }

    function isTaintedVariable(variable) {
      if (!variable) return false
      if (taintCache.has(variable)) return taintCache.get(variable)
      // Guard against cyclic memo re-entry (e.g. `const a = b; const b = a`).
      taintCache.set(variable, false)
      // Only const/parameter defs taint (isTaintedDef). Reaching-assignment taint of a
      // `let`/`var` binding is intentionally NOT modelled — a reassignable binding's value at
      // the use site is flow-sensitive, which the lint does not analyze (immutability
      // boundary). The authoritative backend authz control covers those flows.
      const verdict = variable.defs.some((def) => isTaintedDef(def, variable))
      taintCache.set(variable, verdict)
      return verdict
    }

    /**
     * Is `node` (a call argument, or a value reached through object/array/template
     * literals, spreads, transparent wrappers, value-combining expressions, resolved-const
     * initializers, and const object-literal projections) a caller-supplied identity?
     *
     * This is the SINGLE taint oracle. Recursion descends container/wrapper/combining
     * shapes and, at an Identifier leaf, resolves the binding and (for a `const x = <init>`)
     * recurses into `<init>` — so a tainted identity buried anywhere inside a data-combining
     * expression OR reached through an intermediate `const` is detected identically. It
     * NEVER descends into a function body — `FunctionExpression` / `ArrowFunctionExpression`
     * hit the `default` branch — and it never scans an arbitrary call's argument list,
     * preserving the documented interprocedural / callback-body limit.
     */
    function exprHasTaint(node) {
      if (!node) return false
      switch (node.type) {
        // --- leaf taint sources ---------------------------------------------------
        case 'Identifier':
          return isTaintedVariable(resolveVariable(node))
        case 'MemberExpression':
          // Fires for `props.userId` and (via ChainExpression unwrap) `props?.userId`;
          // isIdentitySourceMember resolves the object position through wrappers/combiners.
          // Also fires for a static const object-literal projection `q.userId`.
          return isIdentitySourceMember(node) || isTaintedObjectProjection(node)
        case 'CallExpression':
          // Fires for `searchParams.get('userId')` and (via ChainExpression unwrap) the
          // optional-chained form. An arbitrary call's arguments are NOT scanned (function
          // boundary): only this recognised leaf shape taints.
          return isSearchParamsGetCall(node)

        // --- literal containers + spreads: descend into value/element/argument slots
        //     only (never a function body) --------------------------------------------
        case 'TemplateLiteral':
          return node.expressions.some((e) => exprHasTaint(e))
        case 'ObjectExpression':
          // Route every entry through the switch so SpreadElement is handled too.
          return node.properties.some((p) => exprHasTaint(p))
        case 'Property':
          return exprHasTaint(node.value)
        case 'ArrayExpression':
          // Elements may be null (holes) or SpreadElement; exprHasTaint handles both.
          return node.elements.some((el) => exprHasTaint(el))
        case 'SpreadElement': // { ...x } / [ ...x ] / f(...x)
          return exprHasTaint(node.argument)

        // --- transparent single-child wrappers: the wrapped value IS the same
        //     caller-supplied expression, so recurse into it -------------------------
        case 'ChainExpression': // props?.userId, searchParams?.get('userId')
        case 'ParenthesizedExpression': // (props.userId) when the parser preserves parens
        case 'TSAsExpression': // props.userId as string
        case 'TSSatisfiesExpression': // props.userId satisfies Id
        case 'TSNonNullExpression': // props.userId!
        case 'TSTypeAssertion': // <string>props.userId
        case 'TSInstantiationExpression': // fn<Id> — child in .expression
          return exprHasTaint(node.expression)
        case 'AwaitExpression':
          // VALUE-PRESERVING: `await <promise-of-identity>` resolves to the identity value
          // itself, so taint propagates through `.argument`. (UnaryExpression used to SHARE
          // this branch — codex round-9 false positive: every unary operator is
          // VALUE-DERIVING and is now handled in the value-deriving group below.)
          return exprHasTaint(node.argument)

        // --- VALUE-COMBINING operators whose RESULT can still BE the caller identity:
        //     a tainted operand that can flow into the combined value propagates -------
        case 'BinaryExpression':
          // Only `+` (string/numeric concatenation) can carry the identity VALUE into the
          // result (`'/x?userId=' + props.userId`). Every other binary operator —
          // equality/relational (`=== !== == != < > <= >= instanceof in`), arithmetic
          // (`- * / % **`), bitwise (`& | ^ << >> >>>`) — reduces its operands to a
          // boolean/number/NaN that CANNOT reproduce the caller-supplied identity, so
          // `id === props.userId` must NOT taint `id` (round-8 false positive: a boolean
          // `enabled` flag on `useQuery` is not the identity).
          return node.operator === '+' && (exprHasTaint(node.left) || exprHasTaint(node.right))
        case 'LogicalExpression': // props.userId ?? useCallerId(), a || b, a && b
          // `??`/`||`/`&&` evaluate to one of their OPERANDS (not a derived boolean/number),
          // so the identity genuinely passes through either side — unchanged from before.
          return exprHasTaint(node.left) || exprHasTaint(node.right)
        case 'ConditionalExpression': // cond ? props.userId : x
          // Only the SELECTED branch's value flows out of a ternary; `.test` is a boolean
          // condition and can never itself be the resulting value, so it must NOT taint
          // (round-8 false positive: `cond === props.userId ? a : b` is not tainted by the
          // test even though the test expression mentions the identity).
          return exprHasTaint(node.consequent) || exprHasTaint(node.alternate)
        case 'SequenceExpression':
          // A comma sequence EVALUATES to its LAST operand ONLY; earlier operands are
          // evaluated-and-discarded, so their value never reaches the call. Recurse into
          // the LAST expression alone: `(sideEffect(), props.userId)` is tainted (the value
          // IS the identity) but `(props.userId, safeId)` is NOT (round-9 sequence fix — a
          // `.some()` over ALL operands false-positived on a discarded non-last element).
          return exprHasTaint(node.expressions[node.expressions.length - 1])
        case 'AssignmentExpression':
          // `id = props.userId` / `id += props.userId` / `id ??= props.userId` — the
          // expression's VALUE is its right-hand side, so recurse right only. (A write
          // TARGET like `props.userId = x` correctly does NOT taint: the value is `x`.)
          return exprHasTaint(node.right)

        // --- VALUE-DERIVING operators: produce a NEW non-identity value (boolean /
        //     number / string / undefined) that can never reproduce the caller-supplied
        //     identity, so taint must NOT propagate through them --------------------------
        case 'UnaryExpression':
          // EVERY unary operator — `!` / `typeof` / `void` / `~` / unary `+` / unary `-` /
          // `delete` — yields boolean/string/number/undefined, never the identity. So
          // `enabled: !!props.userId`, `!props.userId`, `typeof props.userId`,
          // `void props.userId`, `~props.userId`, `+props.userId` must NOT flag (codex
          // round-9 false positive — was sharing AwaitExpression's unconditional recursion).
          // The sibling value-deriving exclusions handled inline above are BinaryExpression
          // with a non-`+` operator and ConditionalExpression's `.test`.
          return false
        case 'UpdateExpression': // `props.userId++` / `--props.userId` → a number
          return false

        default:
          // Everything else — crucially FunctionExpression / ArrowFunctionExpression —
          // is NOT descended: crossing a function boundary is the documented
          // interprocedural / callback-body out-of-scope limit.
          return false
      }
    }

    /** Human-readable identity name for the report message. */
    function describeIdentity(node) {
      // Peel transparent single-child wrappers so `props?.userId` / `(props.userId as
      // string)` name the real key rather than the generic fallback. (Combining nodes —
      // binary/logical/ternary — legitimately have no single identity name.)
      const n = unwrapTransparent(node)
      if (!n) return 'identity'
      if (n.type === 'Identifier') return n.name
      if (n.type === 'MemberExpression') {
        const key = getPropertyKeyName(n.property, n.computed)
        if (key !== null && IDENTITY_NAMES.has(key)) return key
      }
      if (isSearchParamsGetCall(n)) return n.arguments[0].value
      return 'identity'
    }

    return {
      CallExpression(node) {
        if (!isDataCallee(node.callee)) return
        for (const arg of node.arguments) {
          if (!exprHasTaint(arg)) continue
          context.report({
            node: arg,
            messageId: 'callerIdentityFromProps',
            data: { call: calleeName(node.callee), name: describeIdentity(arg) },
          })
          break // one report per call is enough
        }
      },
      // NOTE (round-11): the former TaggedTemplateExpression sink existed solely for the
      // dropped DATA-VERB-NAME heuristic (`` getOrders`…` `` / `` db.query`…` ``). None of
      // the narrowed high-confidence sinks (hooks / fetch / HTTP clients / HTTP-verb member
      // calls) is invoked as a template TAG, so the tag sink shape is gone with the verb set.
    }
  },
}

export default rule
