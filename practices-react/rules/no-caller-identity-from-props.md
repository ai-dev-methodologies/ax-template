---
title: Caller identity for authz-relevant data calls must come from the caller-id hook — never from props, params, searchParams, or a destructured function argument
impact: HIGH
impactDescription: "A component/hook/service function that trusts a userId-shaped value handed in from outside (props, route params, searchParams, a destructured argument) instead of deriving it itself from the caller-id hook / session is the frontend mirror of Broken Object Level Authorization (BOLA/IDOR): whoever controls the incoming prop controls whose data gets fetched, filtered, or queried."
tags:
  - security
  - authz
  - idor
  - bola
  - eslint
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-SECURITY-AUTHZ-001"
verification:
  type: lint
  rule_id: "ax/no-caller-identity-from-props"
  status: shipped
  notes: "Shipped: ax/no-caller-identity-from-props flags userId/currentUserId/actorId/memberId when a PROVENANCE-confirmed caller-supplied source flows into a HIGH-CONFIDENCE data-boundary sink (round-11 narrowed set): (1) the known data-fetching hooks — useSWR family/useQuery/useMutation/useInfiniteQuery/useSuspenseQuery — with identity anywhere in their arguments (queryKey/queryFn config/url/params/body/variables/where); (2) HTTP boundary calls — native fetch(...), the bare callable clients axios(...)/ky(...)/got(...), and HTTP-verb MEMBER calls .get/.post/.put/.patch/.delete (api.get(...), axios.post(...)); (3) within those sinks, identity in any data-selecting position the taint recursion reaches — a ?userId= query-string interpolation in a URL string/template, a where/params/filter/searchParams object field, a queryKey element, a direct/spread argument. The former DATA-VERB-NAME sink heuristic (any callee or tagged-template tag whose name starts with fetch/get/load/query/find/list/search/filter/where) was DROPPED in round-11 as a realistic-false-positive source: it flagged identity passed positionally to arbitrary/local functions — getAvatarColor(userId) (a pure presentation helper), formatName(userId), a custom fetchOrders(userId) wrapper — which a name prefix cannot distinguish from a data boundary; a local data-wrapper is an interprocedural surface, documented out of scope and covered by the authoritative backend control. The soundness boundary is PROVABLE IMMUTABILITY + PROVENANCE (codex round-6, extended round-7): a binding's static value is trusted as a source ONLY when the binding is provably immutable, decided from ESLint scope references (not flow analysis). IN scope = (a) a function's own destructured identity param {userId} and a function PARAMETER named props/params/searchParams, in both cases ONLY while NEVER REASSIGNED — including a destructuring-ASSIGNMENT reassignment ({ x } = …/[props] = …), round-7; (b) a provably-immutable variable resolution — alias, static const-object-literal projection, and resolved-const initializer — where a const OBJECT/ARRAY is trusted only while never PROPERTY-MUTATED (q.x=…/delete q.x/q.x++/nested q.a.b=…/Object.assign(q,…)/a destructuring-ASSIGNMENT member target ({ x: q.x } = …), [q.x] = …, including nested/computed-key/defaulted pattern targets — round-7), since const freezes the binding not the object; (c) a ROUTER-IMPORTED source hook — useParams()/useSearchParams() only when imported from react-router / react-router-dom / next/navigation / next/router. The tainted value is caught as a direct/spread argument, a template-literal interpolation, or an object-argument property at any nesting depth (through arrays, spreads, wrappers, and combiners). OUT of scope (documented heuristic limits, not silent gaps): MUTABLE let/var bindings, any REASSIGNED parameter or PROPERTY-MUTATED const object (dropped conservatively → documented false-negative), mutation through a separate alias or an arbitrary called helper (needs alias/interprocedural analysis), interprocedural helper-indirection, spread-into-object projection, and non-router-imported / locally-defined hooks. This FE lint is DEFENSE-IN-DEPTH; the AUTHORITATIVE BFLA control is the backend authz + the sibling BE rule caller-authentication-only-no-userid-param (the client is untrusted). Round-12 hardening: an HTTP-verb MEMBER call (.get/.post/.put/.patch/.delete) is a sink ONLY when its receiver is a PROVEN HTTP-client object — import-provenance-confirmed (axios/ky/got/ofetch/redaxios), a const bound to a recognized client-factory result (axios.create()/ky.create()/.extend()/got.extend()/ofetch.create()/new XxxApi()/new XxxClient()), or a conventional client name (api/http/client/apiClient/httpClient/request) gated by non-client-value proof (never a Map/Set/WeakMap/WeakSet/object/array literal) — closing the realistic false positive where the verb-member shape alone flagged ordinary collection calls (map.get(props.userId), cache.get(props.userId), set.delete(props.userId)). Wired at error (not warn→promote); a standalone non-vacuous Linter-API sweep (TS parser + non-vacuity canary) across the 6 reference apps + frontend/src + frontend/packages + templates/L1 + templates/L4 is false-positive-free."
provenance: { pilot: false, pipeline_version: "2026-07-20", pipeline_steps: [wave2_consumer_proof_authz_seam, rule_authoring, teeth_proof] }
audit:
  accuracy: { status: verified, last_verified: "2026-07-20" }
  freshness: { status: current, last_verified: "2026-07-20", next_review_by: "2026-10-18" }
  completeness: { status: complete, amendments: ["Scoped to 4 identity names (userId/currentUserId/actorId/memberId) and 3 source names (props/params/searchParams) per the originating task; snake_case query-string variants and arbitrary identity-name conventions are a documented BACKLOG candidate, not silently missed."] }
  gap_check: { status: complete }
upstream:
  - id: cwe-639
    title: "CWE-639: Authorization Bypass Through User-Controlled Key"
    url: "https://cwe.mitre.org/data/definitions/639.html"
    role: seed
evidence:
  - source_type: external
    citation: "CWE-639: Authorization Bypass Through User-Controlled Key — the software relies on caller-supplied input, alone or in combination with a stored key, to select a resource or user account without verifying the caller is authorized to access that resource or account."
    url: "https://cwe.mitre.org/data/definitions/639.html"
    quote: "The system's authorization functionality does not prevent one user from gaining access to another user's data or record by modifying the key value used to identify the data."
    quoted_at: "2026-07-20"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 Broken Object Level Authorization: object IDs are the most common and impactful API attack vector — always implement proper object-level authorization checks based on the user policies and hierarchy, and never rely on the client to supply the object identifier."
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
    quote: "APIs tend to expose endpoints that handle object identifiers, creating a wide attack surface of Object Level Access Control issues. Object level authorization checks should be considered in every function that accesses a data source using an ID from the user."
    quoted_at: "2026-07-20"
sibling_rules: [caller-authentication-only-no-userid-param]
---

## Caller identity for authz-relevant data calls must come from the caller-id hook — never from props, params, searchParams, or a destructured function argument

**Impact: HIGH — This is the frontend mirror of Broken Object Level Authorization. If a function that fetches/filters/queries data trusts whatever `userId` it was handed instead of deriving it from the session itself, whoever controls that prop controls whose data comes back.**

The backend rule `caller-authentication-only-no-userid-param` established the structural fix for this on the API side: never accept `userId` via path or query — derive it from `Authentication` server-side, so there is no parameter for an attacker (or a careless caller) to control. The identical risk exists one layer up the call stack on the frontend: a component, hook, or service function that receives `userId` (or `currentUserId` / `actorId` / `memberId`) as a prop, a Next.js route `params`/`searchParams` value, or any destructured function argument — and then feeds that value into a fetch/filter/query call — has the same structural weakness. The fix is the same shape: derive the caller's identity from `useCallerId()` (or an equivalent session source) **inside** the function that makes the call, not from a value some caller handed in.

### Incorrect — identity comes in via props, used directly

```tsx
// ❌ WRONG — userId is prop-drilled in; whoever renders this component controls
// whose orders get fetched. A parent component with a stale/attacker-influenced
// userId prop silently fetches the wrong user's data.
function OrdersList({ userId }: { userId: string }) {
  const { data } = useSWR(`/api/orders?userId=${userId}`)
  return <OrderTable orders={data} />
}
```

### Incorrect — identity comes from a Next.js route param or searchParams

```tsx
// ❌ WRONG — params/searchParams are caller-controlled request data, not an
// authenticated identity. Anyone can edit the URL.
export default function Page({ params, searchParams }: { params: { userId: string }, searchParams: { userId?: string } }) {
  const { userId } = params
  return fetch(`/api/orders?userId=${userId}`)
}

export default function Page2({ searchParams }: { searchParams: URLSearchParams }) {
  return api.get(searchParams.get('userId'))
}
```

### Incorrect — renamed destructuring does not make it safe

```tsx
// ❌ WRONG — renaming the local binding doesn't change WHERE the value came from.
// The rule follows the extracted key ('userId'), not the local name ('uid').
function OrdersList({ userId: uid }: { userId: string }) {
  const { data } = useQuery({ where: { userId: uid } })
  return <OrderTable orders={data} />
}
```

### Correct — identity is derived from the caller-id hook inside the function

```tsx
// ✅ CORRECT — useCallerId() reads the authenticated session; there is no prop
// for a parent (or an attacker) to influence.
import { useCallerId } from '@/lib/fork-receiver-kit/use-caller-id'

function OrdersList() {
  const userId = useCallerId()
  const { data } = useSWR(`/api/orders?userId=${userId}`)
  return <OrderTable orders={data} />
}
```

```tsx
// ✅ CORRECT — a server component deriving the caller from its own session read,
// not from params/searchParams.
export default async function Page() {
  const userId = useCallerId()
  const orders = await fetch(`/api/orders?userId=${userId}`).then((r) => r.json())
  return <OrderTable orders={orders} />
}
```

### The soundness boundary: provable immutability + provenance

A heuristic lint cannot soundly do flow-sensitive taint through a binding whose value can **change** — the value at a use site depends on statement order, reassignments, and in-place mutations, which needs full dataflow analysis (disproportionate here). So a binding's static value is trusted as a taint source **only when the binding is provably immutable** — decided purely from ESLint scope references (`Variable.references` / `.isWrite()`), not flow analysis. The in-scope surface is exactly that flow-safe one: **provenance-confirmed parameter sources that are never reassigned**, **provably-immutable variable resolution** (a `const` binding can never be reassigned — and for an **object/array** value its static slot is trusted only while the object is never **property-mutated**, since `const` freezes the binding, not the object), and **router-imported source hooks** (detection by import provenance, never by spelling). Everything outside that is a documented out-of-scope limit — and the **authoritative** BFLA/IDOR control is the backend authz plus the sibling backend rule `caller-authentication-only-no-userid-param`; this FE lint is **defense-in-depth** on an untrusted client, so a documented miss here is never an authz hole on its own.

Concretely (codex round-6): a **reassigned parameter** (`function F(props){ props = { userId: useCallerId() }; … }`, or a destructured `function F({ userId }){ userId = auth(); … }`) and a **property-mutated const object** (`const q = { userId: props.userId }; q.userId = useCallerId(); …`) are dropped as sources — their initializer no longer reflects the value at the use site. The taint was previously flow-**insensitive**, so a binding overwritten to a safe value *after* being tainted still false-positived; requiring provable immutability closes that without introducing flow analysis.

Codex round-7 closed a narrower gap in the same mutation check: a write reached through a **destructuring-ASSIGNMENT pattern target** — `({ userId: q.userId } = safe)`, `[q.userId] = safe`, a nested/computed-key/defaulted pattern, or a parameter reassigned that way (`({ x } = …)`, `[props] = …`) — was not recognized as a mutation/reassignment. ESLint scope analysis treats the member's *object* in that shape as a plain read reference (not a write), so `q` stayed trusted and a subsequent `fetchOrders(q)` kept false-positive-flagging even though `q.userId` had just been overwritten to a session value. The rule now also recognizes any of these forms as a mutation/reassignment via a structural walk of the assignment's LHS pattern, in addition to the ESLint-scope-reference check.

### What the rule flags

1. A function's own parameter destructuring that extracts an identity key (`function F({ userId }) {}`) — regardless of parameter position or local rename — when that binding is later used inside a fetch/filter/query call in the same function.
2. Destructuring FROM a **provenance-confirmed source object** (`const { userId } = props`) — a source object is a function parameter named `props`/`params`/`searchParams`, a router-imported `useParams()`/`useSearchParams()` result, or a `const` alias resolving to one — likewise when used in a data call.
3. Member access on a source object — `props.userId`, `params['userId']` (a literal computed key) — used as a call argument, including through a `const` alias (`const p = props; p.userId`) and a `const` object-literal projection (`const q = { userId: props.userId }; q.userId`).
4. `searchParams.get('userId')` on a source object, whether assigned to a `const` first or used inline as the call argument.
5. A tainted identity value appearing as: a direct/spread call argument, a template-literal interpolation (`` `/api/orders?userId=${userId}` `` — the query-string data-selecting position), or an object-argument property at **any nesting depth** — through arrays, nested templates, object/array spreads, transparent wrappers (optional-chain, TS `as`/`!`/`satisfies`), and value combiners (`??`/`||`/`&&`/`?:`/comma) — covering the query-config shape `useQuery({ where: { OR: [{ userId }] } })` and a `queryKey` element.
6. Identity names covered: `userId`, `currentUserId`, `actorId`, `memberId`.
7. Data-boundary sinks covered (round-11 — a high-confidence, FP-safe set ONLY): **(a)** the known data-fetching hooks `useSWR`/`useSWRInfinite`/`useSWRMutation`/`useQuery`/`useMutation`/`useInfiniteQuery`/`useSuspenseQuery` — identity anywhere in their arguments (`queryKey`, the `queryFn`/config object's `url`/`params`/`body`/`variables`/`where` fields, a direct argument); **(b)** HTTP boundary calls — native `fetch(...)`, the bare callable HTTP clients `axios(...)`/`ky(...)`/`got(...)`, and HTTP-verb **member** calls `.get`/`.post`/`.put`/`.patch`/`.delete` (`api.get(...)`, `axios.post(...)`) — but **only when the receiver is a proven HTTP-client object** (round-12, see below).
8. **Receiver provenance for verb-member calls (round-12).** A `.get`/`.post`/`.put`/`.patch`/`.delete` member call counts as an HTTP sink only when its receiver is one of: **(a)** an identifier imported from a known HTTP-client package (`axios`, `ky`, `got`, `ofetch`, `redaxios`) — resolved via scope analysis, so a renamed import (`import myHttp from 'axios'`) still counts; **(b)** a `const` binding initialized from a recognized client-factory call (`axios.create()`, `ky.create()`/`.extend()`, `got.extend()`, `ofetch.create()`, `new XxxApi()`/`new XxxClient()`); **(c)** a conventional client identifier name (`api`, `http`, `client`, `apiClient`, `httpClient`, `request`, or the bare well-known package-root names themselves used without an import) — a **documented heuristic fallback**, applied only when the binding cannot be proven to be a non-client value (`new Map()`/`new Set()`/`new WeakMap()`/`new WeakSet()`/an object literal/an array literal). This closes the round-12 false positive where the verb-member shape alone flagged ordinary collection calls — `map.get(props.userId)`, `cache.get(props.userId)`, `set.delete(props.userId)`.

### What it does NOT flag (documented limits, not silent gaps)

- **Identity passed POSITIONALLY to an arbitrary or local function is not a sink** (codex round-11). The former data-verb-name heuristic (any callee — or tagged-template tag — whose name starts with `fetch`/`get`/`load`/`query`/`find`/`list`/`search`/`filter`/`where`) flagged `getAvatarColor(userId)` (a **pure presentation helper**), `formatName(userId)`, and a custom `fetchOrders(userId)` wrapper identically: a name prefix cannot distinguish a presentation helper from a data boundary. That heuristic is dropped — sinks are now the unambiguous data boundaries of item 7 only. A **local data-wrapper** (`fetchOrders`, an ORM call like `db.orders.findMany(...)`) is an interprocedural surface — whether its body reaches `fetch`/`axios` is a function-boundary question this lint documents as out of scope — and the authoritative BFLA control is the **backend** authz plus the sibling BE rule `caller-authentication-only-no-userid-param`.
- **A MUTABLE `let`/`var` binding is not taint-tracked at all** (codex round-5). Its value at a use site is flow-sensitive: `let uid = props.userId; uid = auth(); use(uid)` is SAFE (tainted init overwritten before use) while `let uid = auth(); use(uid); uid = props.userId` is NOT (write after the use) — a flow-insensitive lint cannot separate the two without a false positive in one direction. So neither a `let uid = props.userId` initializer, a reaching `uid = props.userId` write, nor a `let p = props` alias taints the binding. Only `const` (one definitive initializer) is resolved. The backend authz control covers mutable flows.
- **A REASSIGNED parameter or a PROPERTY-MUTATED const object is dropped as a source** (codex round-6, extended round-7). A parameter overwritten (`props = { userId: useCallerId() }`, a destructured `userId = auth()`, or a destructuring reassignment `({ x } = …)` / `[props] = …`) and a const object mutated in place (`q.userId = …` / `q['userId'] = …` / `delete q.userId` / `q.userId++` / nested `q.a.b = …` / `Object.assign(q, …)` / a destructuring-assignment target `({ x: q.x } = …)` / `[q.x] = …`, including nested/computed-key/defaulted patterns) no longer hold the caller-supplied value at the use site, so the binding is treated conservatively — no flag. A resulting miss is a documented false-negative, never a false positive. Detection scans the binding's own scope references plus a structural walk of destructuring-assignment LHS patterns (round-7); the immutability is provable, not flow-analyzed.
- **Mutation through a SEPARATE alias or an arbitrary called helper is not detected** — `const r = q; r.userId = …; use(q.userId)` (alias mutation) and `mutate(q); use(q.userId)` (interprocedural mutator) would need alias / interprocedural analysis the rule does not attempt, so the original binding still reads as trusted (a possible false positive, but only for a locally-mutated caller-supplied object — itself a React read-only-props anti-pattern — and always backstopped by the authoritative backend authz).
- **An identity read through an intermediate helper function** — `function readId(p) { return p.userId }; const uid = readId(props); api.get(uid)` — is not traced. This requires interprocedural data-flow analysis the rule does not attempt (`exprHasTaint` never descends into a function body or scans an arbitrary call's argument list).
- **A dynamically-computed member key** — `props[someVariable]` where `someVariable` happens to hold `'userId'` at runtime — is not traced; only a literal computed key (`props['userId']`) is caught.
- **Spread-into-object projection** — `const q = { ...tainted }; q.userId`, a `let`-reassigned object, or an interprocedurally-built object — is not projected; static projection is bounded to a DIRECT property of a locally-declared `const` object literal.
- **A hook named `useParams`/`useSearchParams` that is NOT imported from a router package** — a locally-defined `function useParams(){…}` or a same-named import from an unrelated package — is not a source object (detection is by import provenance, not spelling; codex round-5).
- **Identity usage inside an arbitrary callback body** — `items.filter(i => i.ownerId === userId)` — is not scanned; only the data call's own arguments (and reachable container values) are inspected.
- **A destructured/session-derived identity from a non-canonical source** is correctly NOT flagged — `const { userId } = auth()` (the common Clerk-style pattern) or `const props = auth()` (a local reusing the name) — its provenance is not a source object, so it reads as the intended safe pattern.
- **A tainted parameter that is never used in a data call** (e.g. rendered as text, `<span>{userId}</span>`) is not reported — the rule gates on actual data-call usage, not merely on receiving the value.
- **Non-canonical parameter names for props** (a fork renaming the conventional `props`/`params`/`searchParams` identifiers) are not recognized as source objects — a name-based provenance anchor, consistent with the rest of the catalog's static rules (no full type/scope-based caller-graph analysis).
- **An ordinary collection/cache/store member call is not a sink** (codex round-12). `map.get(props.userId)`, `cache.get(props.userId)`, `set.delete(props.userId)` — and any other `.get`/`.post`/`.put`/`.patch`/`.delete` call whose receiver is not a proven HTTP-client object — are normal JS constructs, not HTTP boundaries. The receiver must be import-provenance-confirmed, a recognized client-factory result, or a conventional client name not provably a Map/Set/object/array literal (see item 8 above).

Reference: [CWE-639: Authorization Bypass Through User-Controlled Key](https://cwe.mitre.org/data/definitions/639.html)

Reference: [OWASP API Security Top 10 (2023) — API1:2023 BOLA](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)

Reference: [practices/rules/caller-authentication-only-no-userid-param.md](../../practices/rules/caller-authentication-only-no-userid-param.md) — backend mirror.
