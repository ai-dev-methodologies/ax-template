---
sentinel:
  source_concat_sha256: "48196b825102a79a7cf07d8966084512587777430f392f0f23818a036267c0f3"
  rule_count: 108
  generated_by: "practices/generate_index.sh"
---

# practices-react — Rule INDEX (auto-generated)

## By tag
- **a11y** (5) — billing-frontend-status-color-and-table-a11y, file-storage-frontend-render-a11y-error, impersonation-banner-required-when-acting-as-other-user, rate-limit-must-surface-to-user, ux-block-uses-design-tokens-and-a11y
- **accessibility** (4) — billing-frontend-status-color-and-table-a11y, combobox-respects-hangul-ime-composition, file-storage-frontend-render-a11y-error, ux-block-uses-design-tokens-and-a11y
- **activity** (1) — rendering-activity
- **admin** (3) — feature-flags-frontend-admin-toggle, impersonation-banner-required-when-acting-as-other-user, no-impersonation-bypass-via-helper-rename
- **advanced** (2) — advanced-event-handler-refs, advanced-use-latest
- **after** (1) — server-after-nonblocking
- **algorithms** (1) — js-min-max-loop
- **analytics** (2) — bundle-defer-third-party, server-after-nonblocking
- **animation** (1) — rendering-animate-svg-wrapper
- **api-routes** (1) — async-api-routes
- **app-router** (1) — next-async-params-parallel
- **app-startup** (1) — advanced-init-once
- **architecture** (8) — no-cross-feature-deep-import, no-feature-internal-import, no-god-route, no-l4-cross-import, no-route-client-data-fetching, no-server-state-in-local-state, no-upward-layer-import, prefer-recipe-over-l4-page-cross-import
- **arrays** (5) — js-combine-iterations, js-flatmap-filter, js-length-check-first, js-min-max-loop, js-tosorted-immutable
- **assets** (1) — rendering-svg-precision
- **async** (7) — async-defer-await, async-dependencies, async-parallel, async-suspense-boundaries, next-async-params-parallel, rendering-script-defer-async, server-after-nonblocking
- **audit-log** (1) — audit-log-frontend-viewer-rbac-virtualized
- **auth** (1) — auth-frontend-pages-bind-auth-contract
- **authentication** (1) — server-auth-actions
- **authorization** (1) — server-auth-actions
- **authz** (1) — no-caller-identity-from-props
- **await** (1) — async-defer-await
- **b2b** (1) — business-registration-checksum-required
- **barrel-files** (1) — bundle-barrel-imports
- **billing** (3) — billing-frontend-status-color-and-table-a11y, currency-amount-no-raw-jsx-render, no-billing-payment-ui-boundary
- **bola** (1) — no-caller-identity-from-props
- **boundary** (1) — no-billing-payment-ui-boundary
- **bundle** (5) — bundle-barrel-imports, bundle-conditional, bundle-defer-third-party, bundle-dynamic-imports, bundle-preload
- **business-registration** (1) — business-registration-checksum-required
- **cache** (6) — js-cache-function-results, nextjs-use-cache, nextjs-use-cache-private, nextjs-use-cache-remote, server-cache-lru, server-cache-react
- **cache-components** (3) — nextjs-use-cache, nextjs-use-cache-private, nextjs-use-cache-remote
- **caching** (2) — js-cache-property-access, js-cache-storage
- **callbacks** (1) — rerender-functional-setstate
- **catalog** (1) — practices-frontend-catalog-browser
- **checkout** (1) — payment-frontend-checkout-idempotent-states
- **checksum** (1) — business-registration-checksum-required
- **client** (5) — client-event-listeners, client-localstorage-schema, client-passive-event-listeners, client-swr-dedup, no-server-state-in-local-state
- **client-server-boundary** (1) — no-route-client-data-fetching
- **closures** (1) — rerender-functional-setstate
- **code-splitting** (1) — bundle-dynamic-imports
- **codification** (1) — ux-block-uses-design-tokens-and-a11y
- **combobox** (1) — combobox-respects-hangul-ime-composition
- **command-palette** (1) — search-frontend-palette-highlight
- **comparison** (1) — js-length-check-first
- **components** (2) — no-app-local-ui-primitives, rerender-no-inline-components
- **composition** (1) — server-parallel-fetching
- **concurrent** (2) — rerender-transitions, rerender-use-deferred-value
- **conditional** (2) — async-defer-await, rendering-conditional-render
- **conditional-loading** (1) — bundle-conditional
- **confirm-dialog** (1) — dsr-frontend-pages-bind-dsr-contract
- **consistency** (1) — no-app-local-ui-primitives
- **content-visibility** (1) — rendering-content-visibility
- **contract-first** (6) — audit-log-frontend-viewer-rbac-virtualized, auth-frontend-pages-bind-auth-contract, crud-frontend-pages-bind-crud-contract, dsr-frontend-pages-bind-dsr-contract, notification-frontend-inbox-settings-bell, payment-frontend-checkout-idempotent-states
- **control-flow** (1) — js-early-exit
- **correctness** (3) — rendering-conditional-render, rerender-functional-setstate, rerender-no-inline-components
- **cross-import** (1) — no-billing-payment-ui-boundary
- **cross-request** (1) — server-cache-lru
- **crud** (1) — crud-frontend-pages-bind-crud-contract
- **csp** (1) — rendering-hydration-no-flicker
- **css** (3) — js-batch-dom-css, rendering-animate-svg-wrapper, rendering-content-visibility
- **currency** (2) — currency-amount-no-raw-jsx-render, locale-aware-number-date-format
- **cwv** (1) — virtualized-table-when-rowcount-gt-1000
- **data-blocks** (1) — l2-prefer-data-prop-over-direct-fetch
- **data-fetching** (3) — client-swr-dedup, l2-prefer-data-prop-over-direct-fetch, no-route-client-data-fetching
- **data-minimization** (1) — client-localstorage-schema
- **data-structures** (1) — js-set-map-lookups
- **data-table** (1) — crud-frontend-pages-bind-crud-contract
- **datatable** (1) — virtualized-table-when-rowcount-gt-1000
- **date** (1) — locale-aware-number-date-format
- **decoupling** (2) — l2-prefer-data-prop-over-direct-fetch, l2-prefer-onsubmit-prop
- **deduplication** (2) — client-swr-dedup, server-cache-react
- **defer** (2) — bundle-defer-third-party, rendering-script-defer-async
- **dependencies** (4) — async-dependencies, rerender-dependencies, rerender-move-effect-to-event, rerender-split-combined-hooks
- **derived-state** (2) — rerender-derived-state, rerender-derived-state-no-effect
- **design-system** (1) — no-app-local-ui-primitives
- **design-tokens** (1) — ux-block-uses-design-tokens-and-a11y
- **display** (1) — currency-amount-no-raw-jsx-render
- **dom** (1) — js-batch-dom-css
- **domain-isolation** (2) — no-l4-cross-import, prefer-recipe-over-l4-page-cross-import
- **dsr** (1) — dsr-frontend-pages-bind-dsr-contract
- **dynamic-import** (1) — bundle-dynamic-imports
- **early-return** (2) — async-defer-await, js-early-exit
- **effects** (1) — advanced-use-latest
- **error-boundary** (1) — audit-log-frontend-viewer-rbac-virtualized
- **error-handling** (3) — file-storage-frontend-render-a11y-error, rate-limit-must-surface-to-user, traceid-propagated-client
- **eslint** (7) — no-caller-identity-from-props, no-cross-feature-deep-import, no-feature-internal-import, no-god-route, no-route-client-data-fetching, no-server-state-in-local-state, no-upward-layer-import
- **event-handlers** (1) — advanced-event-handler-refs
- **event-listeners** (2) — client-event-listeners, client-passive-event-listeners
- **events** (1) — rerender-move-effect-to-event
- **experimental** (1) — nextjs-use-cache-private
- **falsy-values** (1) — rendering-conditional-render
- **feature-flags** (2) — feature-flags-frontend-admin-toggle, prefer-feature-gate-over-env-check
- **feature-gates** (1) — bundle-conditional
- **feature-isolation** (1) — no-cross-feature-deep-import
- **feature-layout** (5) — no-cross-feature-deep-import, no-feature-internal-import, no-god-route, no-route-client-data-fetching, no-upward-layer-import
- **file-storage** (1) — file-storage-frontend-render-a11y-error
- **filter** (1) — js-flatmap-filter
- **financial** (1) — payment-frontend-checkout-idempotent-states
- **flatMap** (1) — js-flatmap-filter
- **flicker** (1) — rendering-hydration-no-flicker
- **form-blocks** (1) — l2-prefer-onsubmit-prop
- **form-validation** (1) — business-registration-checksum-required
- **formatting** (1) — locale-aware-number-date-format
- **forms** (5) — auth-frontend-pages-bind-auth-contract, crud-frontend-pages-bind-crud-contract, no-rrn-display-without-legal-basis-gate, no-rrn-in-form-fields, notification-frontend-inbox-settings-bell
- **frontend** (12) — audit-log-frontend-viewer-rbac-virtualized, auth-frontend-pages-bind-auth-contract, billing-frontend-status-color-and-table-a11y, crud-frontend-pages-bind-crud-contract, dsr-frontend-pages-bind-dsr-contract, feature-flags-frontend-admin-toggle, file-storage-frontend-render-a11y-error, locale-aware-number-date-format, notification-frontend-inbox-settings-bell, payment-frontend-checkout-idempotent-states, practices-frontend-catalog-browser, search-frontend-palette-highlight
- **functions** (1) — js-early-exit
- **gdpr** (1) — dsr-frontend-pages-bind-dsr-contract
- **hangul** (1) — combobox-respects-hangul-ime-composition
- **highlighting** (1) — search-frontend-palette-highlight
- **hooks** (4) — advanced-event-handler-refs, advanced-use-latest, rerender-functional-setstate, rerender-lazy-state-init
- **hover** (1) — bundle-preload
- **hydration** (2) — rendering-hydration-no-flicker, rendering-hydration-suppress-warning
- **i18n** (2) — locale-aware-number-date-format, no-hardcoded-user-facing-string-in-l4
- **idempotency** (1) — payment-frontend-checkout-idempotent-states
- **identity** (1) — no-rrn-display-without-legal-basis-gate
- **idor** (1) — no-caller-identity-from-props
- **ime** (1) — combobox-respects-hangul-ime-composition
- **immutability** (1) — js-tosorted-immutable
- **impersonation** (2) — impersonation-banner-required-when-acting-as-other-user, no-impersonation-bypass-via-helper-rename
- **imports** (5) — bundle-barrel-imports, no-cross-feature-deep-import, no-feature-internal-import, no-l4-cross-import, no-upward-layer-import
- **indexing** (1) — js-index-maps
- **initialization** (2) — advanced-init-once, rerender-lazy-state-init
- **integer-minor-units** (1) — currency-amount-no-raw-jsx-render
- **io** (1) — server-hoist-static-io
- **javascript** (13) — js-batch-dom-css, js-cache-function-results, js-cache-property-access, js-cache-storage, js-combine-iterations, js-early-exit, js-flatmap-filter, js-hoist-regexp, js-index-maps, js-length-check-first, js-min-max-loop, js-set-map-lookups, js-tosorted-immutable
- **jsx** (2) — rendering-conditional-render, rendering-hoist-jsx
- **korean** (2) — combobox-respects-hangul-ime-composition, no-hardcoded-user-facing-string-in-l4
- **korean-compliance** (3) — business-registration-checksum-required, no-rrn-display-without-legal-basis-gate, no-rrn-in-form-fields
- **l1-component** (2) — combobox-respects-hangul-ime-composition, rich-content-must-use-dynamic-import
- **l2-block** (6) — impersonation-banner-required-when-acting-as-other-user, no-hardcoded-user-facing-string-in-l4, no-impersonation-bypass-via-helper-rename, prefer-feature-gate-over-env-check, saved-view-must-be-url-state-or-server-persisted, virtualized-table-when-rowcount-gt-1000
- **l2-blocks** (2) — rate-limit-must-surface-to-user, ux-block-uses-design-tokens-and-a11y
- **l2-layer** (2) — l2-prefer-data-prop-over-direct-fetch, l2-prefer-onsubmit-prop
- **l4** (1) — no-billing-payment-ui-boundary
- **l4-layer** (2) — no-l4-cross-import, prefer-recipe-over-l4-page-cross-import
- **l4-template** (3) — no-hardcoded-user-facing-string-in-l4, prefer-feature-gate-over-env-check, rich-content-must-use-dynamic-import
- **layering** (3) — no-cross-feature-deep-import, no-feature-internal-import, no-upward-layer-import
- **layout-thrashing** (1) — js-batch-dom-css
- **lazy-loading** (1) — bundle-conditional
- **loading** (1) — rendering-usetransition-loading
- **localStorage** (4) — client-localstorage-schema, js-cache-storage, rendering-hydration-no-flicker, rerender-defer-reads
- **locale** (2) — locale-aware-number-date-format, no-hardcoded-user-facing-string-in-l4
- **locked_constraint** (2) — no-rrn-display-without-legal-basis-gate, no-rrn-in-form-fields
- **logging** (1) — server-after-nonblocking
- **long-lists** (1) — rendering-content-visibility
- **loops** (2) — js-cache-property-access, js-combine-iterations
- **lru** (1) — server-cache-lru
- **map** (2) — js-index-maps, js-set-map-lookups
- **markdown** (1) — rich-content-must-use-dynamic-import
- **media-query** (1) — rerender-derived-state
- **memo** (2) — rerender-memo, rerender-memo-with-default-value
- **memoization** (2) — js-cache-function-results, js-hoist-regexp
- **middleware** (1) — feature-flags-frontend-admin-toggle
- **module-scope** (1) — server-hoist-static-io
- **monorepo** (1) — no-app-local-ui-primitives
- **mutation** (1) — js-tosorted-immutable
- **navigation** (1) — practices-frontend-catalog-browser
- **next-dynamic** (1) — bundle-dynamic-imports
- **nextjs** (12) — async-api-routes, async-parallel, bundle-barrel-imports, l2-prefer-onsubmit-prop, next-async-params-parallel, nextjs-use-cache, nextjs-use-cache-private, nextjs-use-cache-remote, prefer-recipe-over-l4-page-cross-import, rendering-hydration-suppress-warning, rich-content-must-use-dynamic-import, traceid-propagated-client
- **notification** (1) — notification-frontend-inbox-settings-bell
- **oauth** (1) — auth-frontend-pages-bind-auth-contract
- **observability** (2) — rate-limit-must-surface-to-user, traceid-propagated-client
- **optimistic-update** (1) — feature-flags-frontend-admin-toggle
- **optimization** (18) — advanced-event-handler-refs, advanced-use-latest, async-defer-await, js-cache-property-access, js-early-exit, js-hoist-regexp, js-index-maps, rendering-hoist-jsx, rendering-svg-precision, rerender-defer-reads, rerender-dependencies, rerender-derived-state, rerender-memo, rerender-memo-with-default-value, rerender-simple-expression-in-memo, rerender-split-combined-hooks, rerender-use-deferred-value, server-dedup-props
- **pagination** (1) — crud-frontend-pages-bind-crud-contract
- **parallel-fetching** (1) — server-parallel-fetching
- **parallelization** (4) — async-api-routes, async-dependencies, async-parallel, next-async-params-parallel
- **params** (1) — next-async-params-parallel
- **payment** (2) — no-billing-payment-ui-boundary, payment-frontend-checkout-idempotent-states
- **performance** (21) — bundle-barrel-imports, client-event-listeners, client-passive-event-listeners, js-batch-dom-css, js-cache-function-results, js-cache-storage, js-combine-iterations, js-flatmap-filter, js-index-maps, js-length-check-first, js-min-max-loop, js-set-map-lookups, rendering-animate-svg-wrapper, rendering-script-defer-async, rerender-lazy-state-init, rerender-memo, rerender-no-inline-components, rerender-transitions, rerender-use-ref-transient-values, server-hoist-static-io, virtualized-table-when-rowcount-gt-1000
- **pii** (2) — no-rrn-display-without-legal-basis-gate, no-rrn-in-form-fields
- **practices** (1) — practices-frontend-catalog-browser
- **precision** (1) — currency-amount-no-raw-jsx-render
- **preconnect** (1) — rendering-resource-hints
- **prefetch** (2) — bundle-preload, rendering-resource-hints
- **preload** (2) — bundle-preload, rendering-resource-hints
- **privacy** (3) — dsr-frontend-pages-bind-dsr-contract, no-rrn-display-without-legal-basis-gate, no-rrn-in-form-fields
- **process-env** (1) — prefer-feature-gate-over-env-check
- **promise-graph** (1) — async-dependencies
- **promises** (1) — async-parallel
- **props** (2) — server-dedup-props, server-serialization
- **public-api** (1) — no-feature-internal-import
- **rate-limit** (1) — rate-limit-must-surface-to-user
- **rbac** (1) — audit-log-frontend-viewer-rbac-virtualized
- **react** (6) — advanced-event-handler-refs, async-parallel, bundle-barrel-imports, js-tosorted-immutable, rerender-functional-setstate, rerender-lazy-state-init
- **react-19** (3) — rendering-activity, rendering-resource-hints, rendering-usetransition-loading
- **react-cache** (1) — server-cache-react
- **react-compiler** (3) — rendering-hoist-jsx, rerender-memo, rerender-simple-expression-in-memo
- **react-lazy** (1) — bundle-dynamic-imports
- **recipe-composition** (1) — prefer-recipe-over-l4-page-cross-import
- **reflow** (1) — js-batch-dom-css
- **refs** (1) — advanced-event-handler-refs
- **regexp** (1) — js-hoist-regexp
- **remount** (1) — rerender-no-inline-components
- **rendering** (12) — file-storage-frontend-render-a11y-error, rendering-activity, rendering-animate-svg-wrapper, rendering-conditional-render, rendering-content-visibility, rendering-hoist-jsx, rendering-hydration-no-flicker, rendering-hydration-suppress-warning, rendering-resource-hints, rendering-script-defer-async, rendering-svg-precision, rendering-usetransition-loading
- **rerender** (13) — rerender-defer-reads, rerender-dependencies, rerender-derived-state, rerender-derived-state-no-effect, rerender-memo, rerender-memo-with-default-value, rerender-move-effect-to-event, rerender-no-inline-components, rerender-simple-expression-in-memo, rerender-split-combined-hooks, rerender-transitions, rerender-use-deferred-value, rerender-use-ref-transient-values
- **resilience** (1) — rate-limit-must-surface-to-user
- **resource-hints** (1) — rendering-resource-hints
- **reuse** (1) — no-app-local-ui-primitives
- **rich-content** (1) — rich-content-must-use-dynamic-import
- **routing** (3) — no-god-route, no-route-client-data-fetching, practices-frontend-catalog-browser
- **rrn** (2) — no-rrn-display-without-legal-basis-gate, no-rrn-in-form-fields
- **rsc** (6) — nextjs-use-cache, rich-content-must-use-dynamic-import, server-cache-react, server-dedup-props, server-parallel-fetching, server-serialization
- **runtime-control** (1) — prefer-feature-gate-over-env-check
- **saved-view** (1) — saved-view-must-be-url-state-or-server-persisted
- **script** (1) — rendering-script-defer-async
- **scripts** (1) — bundle-defer-third-party
- **scrolling** (1) — client-passive-event-listeners
- **search** (1) — search-frontend-palette-highlight
- **searchParams** (1) — rerender-defer-reads
- **security** (5) — impersonation-banner-required-when-acting-as-other-user, no-caller-identity-from-props, no-impersonation-bypass-via-helper-rename, server-auth-actions, server-serialization
- **serialization** (2) — server-dedup-props, server-serialization
- **server** (11) — nextjs-use-cache, nextjs-use-cache-private, nextjs-use-cache-remote, server-after-nonblocking, server-auth-actions, server-cache-lru, server-cache-react, server-dedup-props, server-hoist-static-io, server-parallel-fetching, server-serialization
- **server-actions** (4) — async-api-routes, l2-prefer-onsubmit-prop, server-auth-actions, traceid-propagated-client
- **server-components** (1) — async-suspense-boundaries
- **server-state** (2) — client-swr-dedup, no-server-state-in-local-state
- **session** (1) — auth-frontend-pages-bind-auth-contract
- **set** (1) — js-set-map-lookups
- **side-effects** (3) — advanced-init-once, rerender-move-effect-to-event, server-after-nonblocking
- **size-heuristic** (1) — no-god-route
- **sorting** (1) — js-min-max-loop
- **ssr** (3) — rendering-hydration-no-flicker, rendering-hydration-suppress-warning, rich-content-must-use-dynamic-import
- **startTransition** (1) — rerender-transitions
- **state** (4) — js-tosorted-immutable, rendering-usetransition-loading, rerender-derived-state-no-effect, rerender-use-ref-transient-values
- **state-boundary** (1) — no-server-state-in-local-state
- **state-preservation** (1) — rendering-activity
- **static** (1) — rendering-hoist-jsx
- **static-assets** (1) — server-hoist-static-io
- **status-badge** (1) — billing-frontend-status-color-and-table-a11y
- **storage** (2) — client-localstorage-schema, js-cache-storage
- **streaming** (1) — async-suspense-boundaries
- **subscription** (1) — client-event-listeners
- **suspense** (1) — async-suspense-boundaries
- **svg** (2) — rendering-animate-svg-wrapper, rendering-svg-precision
- **svgo** (1) — rendering-svg-precision
- **swr** (1) — client-swr-dedup
- **table** (1) — saved-view-must-be-url-state-or-server-persisted
- **tables** (1) — billing-frontend-status-color-and-table-a11y
- **tanstack-query** (2) — client-swr-dedup, l2-prefer-data-prop-over-direct-fetch
- **theming** (1) — ux-block-uses-design-tokens-and-a11y
- **third-party** (1) — bundle-defer-third-party
- **touch** (1) — client-passive-event-listeners
- **tracing** (1) — traceid-propagated-client
- **transitions** (2) — rendering-usetransition-loading, rerender-transitions
- **tree-shaking** (1) — bundle-barrel-imports
- **url-state** (1) — saved-view-must-be-url-state-or-server-persisted
- **use-cache** (1) — nextjs-use-cache
- **use-cache-private** (1) — nextjs-use-cache-private
- **use-cache-remote** (1) — nextjs-use-cache-remote
- **use-hook** (1) — async-suspense-boundaries
- **useCallback** (1) — rerender-functional-setstate
- **useDeferredValue** (1) — rerender-use-deferred-value
- **useEffect** (5) — advanced-init-once, rerender-dependencies, rerender-derived-state-no-effect, rerender-move-effect-to-event, rerender-split-combined-hooks
- **useEffectEvent** (1) — advanced-use-latest
- **useMemo** (3) — rerender-memo, rerender-simple-expression-in-memo, rerender-split-combined-hooks
- **useState** (2) — rerender-functional-setstate, rerender-lazy-state-init
- **useTransition** (1) — rendering-usetransition-loading
- **user-intent** (1) — bundle-preload
- **useref** (1) — rerender-use-ref-transient-values
- **ux** (1) — ux-block-uses-design-tokens-and-a11y
- **versioning** (1) — client-localstorage-schema
- **virtualization** (3) — audit-log-frontend-viewer-rbac-virtualized, notification-frontend-inbox-settings-bell, virtualized-table-when-rowcount-gt-1000
- **visibility** (1) — rendering-activity
- **vite** (1) — bundle-barrel-imports
- **waterfalls** (3) — async-api-routes, async-parallel, server-parallel-fetching
- **wheel** (1) — client-passive-event-listeners
- **wysiwyg** (1) — rich-content-must-use-dynamic-import
- **xss-safety** (1) — search-frontend-palette-highlight
- **you-might-not-need-an-effect** (2) — rerender-derived-state-no-effect, rerender-move-effect-to-event

## Rules
| id | impact | verification | title |
|---|---|---|---|
| advanced-event-handler-refs | LOW | review | Store event handlers in refs as a fallback when useEffectEvent is unavailable |
| advanced-init-once | LOW-MEDIUM | review | Initialize app-wide state once at module scope, not inside a component's useEffect |
| advanced-use-latest | LOW | review | Use useEffectEvent (React 19.2+) for non-reactive callbacks inside Effects |
| async-api-routes | HIGH | review | API route / Server Action specialization — auth gate first, then start independent work eagerly |
| async-defer-await | HIGH | review | Move `await` into the branch that actually uses the result; place cheap guards first |
| async-dependencies | HIGH | review | For partial-dependency graphs, chain dependent promises and aggregate with Promise.all |
| async-parallel | HIGH | eslint | Initiate independent promises early, then await with Promise.all (or allSettled) |
| async-suspense-boundaries | HIGH | review | Stream wrapper UI fast — fetch in Server Components, pass promises down, resolve with use() inside Suspense |
| audit-log-frontend-viewer-rbac-virtualized | MEDIUM | review | The audit-log viewer UI must virtualize large lists, filter/paginate, gate export behind RBAC, and degrade with empty/error states |
| auth-frontend-pages-bind-auth-contract | HIGH | review | Auth UI pages must realize the auth API contract — each page renders its documented fields as controlled inputs and calls its documented endpoint, with route gating, transparent token refresh, and logout cleanup |
| billing-frontend-status-color-and-table-a11y | MEDIUM | review | Billing UI status badges must pair semantic color with a text label, and pricing tables must use accessible headers (scope=col) + ARIA-labeled CTAs |
| bundle-barrel-imports | HIGH | eslint | Avoid expensive package barrel imports when the bundler does not already optimize them |
| bundle-conditional | HIGH | review | Load feature modules only when the feature is activated |
| bundle-defer-third-party | MEDIUM | review | Defer non-critical third-party SDK init or script loading until after hydration |
| bundle-dynamic-imports | HIGH | review | Lazy-load heavy client-only components via React.lazy/Suspense or next/dynamic |
| bundle-preload | MEDIUM | review | Prefetch heavy modules on strong user-intent signals (hover, focus, viewport, likely next step) |
| business-registration-checksum-required | HIGH | review | Frontend must validate 사업자등록번호 (Business Registration Number) checksum using the NTS algorithm before accepting the value |
| client-event-listeners | LOW | review | One global event listener per (target, event, options); many subscribers |
| client-localstorage-schema | MEDIUM | review | Version localStorage keys, wrap every access in try-catch, store minimal fields |
| client-passive-event-listeners | MEDIUM | review | Use passive listeners on touch/wheel events that do not need preventDefault |
| client-swr-dedup | MEDIUM-HIGH | review | Deduplicate client-side server-state requests with a server-state cache (SWR / TanStack Query / RTK Query / framework primitive) |
| combobox-respects-hangul-ime-composition | HIGH | review | Combobox / autocomplete must suppress onChange filtering during IME composition (한글 IME guard) |
| crud-frontend-pages-bind-crud-contract | MEDIUM | review | CRUD UI pages must realize the CRUD contract — server-paginated list with filter/empty/bulk states, create→redirect, detail with audit fields, edit pre-populated, delete behind a confirm dialog |
| currency-amount-no-raw-jsx-render | CRITICAL | review | All monetary amounts in billing UI must be displayed via CurrencyFormatter using integer minor-unit values; raw number display and float arithmetic are prohibited |
| dsr-frontend-pages-bind-dsr-contract | HIGH | review | DSR (data-subject-rights) UI must realize the GDPR rights contract — dashboard with SLA, access/rectify/portability flows, and destructive erasure/restrict behind a confirm dialog |
| feature-flags-frontend-admin-toggle | MEDIUM | review | Feature-flag admin UI must list flags with a toggle that PATCHes optimistically, an editable detail page, and server-side middleware evaluation |
| file-storage-frontend-render-a11y-error | HIGH | review | File-storage UI must render the documented file surfaces with human-readable sizes, accessible dropzone + status, mapped error messages, and virtualized large lists |
| impersonation-banner-required-when-acting-as-other-user | HIGH | script | ImpersonationBanner must render whenever session.actingAs is non-null |
| js-batch-dom-css | MEDIUM | review | Group DOM writes before reads; prefer className over imperative inline style |
| js-cache-function-results | LOW-MEDIUM | review | Memoize pure deterministic function results in a bounded module-level Map; never store user/tenant-scoped data |
| js-cache-property-access | LOW | review | Cache deep stable property paths outside hot loops; length caching is mostly noise on modern engines |
| js-cache-storage | LOW-MEDIUM | review | Cache repeated synchronous browser-storage reads in memory; invalidate on local writes and cross-tab storage events |
| js-combine-iterations | LOW-MEDIUM | review | Combine multiple .filter/.map passes over the same array into one loop when the array is large or hot |
| js-early-exit | LOW-MEDIUM | review | Return on first failure unless the API contract requires collecting all errors |
| js-flatmap-filter | LOW-MEDIUM | review | Prefer flatMap over map().filter(Boolean) — semantic clarity + single pass |
| js-hoist-regexp | LOW-MEDIUM | review | Hoist static RegExp to module scope; for prop-dependent regex use useMemo; beware /g lastIndex |
| js-index-maps | LOW-MEDIUM | review | Index-by-id Map for joining two collections — O(n²) .find loops become O(n) |
| js-length-check-first | MEDIUM-HIGH | review | Cheap length compare before expensive array equality (sort, serialize, deep compare) |
| js-min-max-loop | LOW | review | Single-pass loop for min/max — O(n) instead of O(n log n) sort; Math.min/max spread only for small arrays |
| js-set-map-lookups | LOW-MEDIUM | eslint | Use Set/Map for repeated membership lookups |
| js-tosorted-immutable | MEDIUM-HIGH | lint | Use ES2023 immutable array methods (.toSorted/.toReversed/.toSpliced/.with) for React state and props |
| l2-prefer-data-prop-over-direct-fetch | HIGH | review | L2 data blocks — receive data as prop; never call fetch() or useQuery() inline |
| l2-prefer-onsubmit-prop | HIGH | review | L2 form blocks — accept onSubmit prop; never import server actions directly |
| locale-aware-number-date-format | MEDIUM | guard | Frontend number/currency/date display MUST use Intl.NumberFormat / Intl.DateTimeFormat, never raw toLocaleString() or manual date-string concatenation |
| next-async-params-parallel | MEDIUM | review | Next.js 16 async params — await params alongside independent server work, not before |
| nextjs-use-cache | HIGH | review | Use the 'use cache' directive for Next.js 16 Cache Components persistent caching |
| nextjs-use-cache-private | LOW | review | use cache: private — experimental escape hatch; refactor runtime APIs out of cached scopes first; not production-recommended in 16.2.6 |
| nextjs-use-cache-remote | HIGH | review | use cache: remote — shared durable caching across server instances; gate on hit-rate and cost first |
| no-app-local-ui-primitives | HIGH | lint | Per-persona apps must reuse the shared catalog (@ax/ui / @ax/blocks) — never define app-local UI primitives |
| no-billing-payment-ui-boundary | HIGH | review | billing UI components must not import from payment UI components and vice versa; the L4/billing ↔ L4/payment boundary is enforceable via the project ESLint config (import/no-restricted-paths) |
| no-caller-identity-from-props | HIGH | lint | Caller identity for authz-relevant data calls must come from the caller-id hook — never from props, params, searchParams, or a destructured function argument |
| no-cross-feature-deep-import | HIGH | lint | A feature must not deep-import another feature's internals — cross-feature reuse goes through the target's barrel or the shared kernel |
| no-feature-internal-import | HIGH | lint | Outside a feature, import it only through its published barrel — never deep into a slice's internals |
| no-god-route | MEDIUM | lint | A "use client" route file should stay thin — a route that grows past a line threshold likely belongs in a feature container |
| no-hardcoded-user-facing-string-in-l4 | HIGH | regex_scan | User-facing strings in L4 templates must use t() — no hardcoded Korean or natural-language literals |
| no-impersonation-bypass-via-helper-rename | HIGH | script | Impersonation bypass via helper rename is not permitted |
| no-l4-cross-import | HIGH | review | L4 domain pages must not import from other L4 domains |
| no-route-client-data-fetching | HIGH | lint | A "use client" route file must not call client data-fetching hooks or raw fetch directly — delegate to a feature hook |
| no-rrn-display-without-legal-basis-gate | CRITICAL | review | Frontend components must not collect or display raw RRN (주민등록번호) fields without an explicit legal-basis disclosure gate |
| no-rrn-in-form-fields | CRITICAL | review | Frontend forms must not include RRN (주민등록번호) input fields by default |
| no-server-state-in-local-state | MEDIUM | lint | Do not seed useState with a query/SWR result's .data — the query cache is the source of truth |
| no-upward-layer-import | HIGH | lint | Layers are single-direction (app -> features -> shared) — a module must never import from a higher layer |
| notification-frontend-inbox-settings-bell | MEDIUM | review | Notification UI must realize the notification contract — virtualized inbox with status filter, mark-read/dismiss actions, preference toggles (partial update), and an unread-count bell |
| payment-frontend-checkout-idempotent-states | HIGH | review | Payment UI must realize the payment contract — checkout with method picker + idempotency-key handler + slow-provider warning, idempotent success/failure pages, methods list/detail, refund |
| practices-frontend-catalog-browser | LOW | review | The practices catalog browser UI must list both catalogs with counts, filter by category, render rule detail with metadata, and 404 unknown rules |
| prefer-feature-gate-over-env-check | HIGH | regex_scan | Feature flag checks must use FeatureGate or the feature-flags API — not process.env |
| prefer-recipe-over-l4-page-cross-import | HIGH | script | When a Next.js page implements a multi-L4 composition matching a Business Pattern Recipe, the L4 domain README must declare applied_recipe; ad-hoc cross-L4 hook/store imports without that declaration are prohibited |
| rate-limit-must-surface-to-user | HIGH | review | A client surface that receives a 429 (Too Many Requests) response must surface the rate-limit condition to the user (RateLimitBannerProvider or an equivalent visible status surface) instead of failing silently or blindly retrying |
| rendering-activity | MEDIUM | review | Use Activity (React 19.2+) for expensive UI that toggles visibility frequently — be aware hidden mode unmounts effects |
| rendering-animate-svg-wrapper | LOW | review | For whole-SVG transform/opacity animations, animate a wrapper div instead of the <svg> element |
| rendering-conditional-render | LOW | lint | For numeric/falsy-tricky conditions use ternary or explicit boolean cast; `&&` is fine for real booleans |
| rendering-content-visibility | HIGH | review | Use content-visibility for long static sections, paired with realistic contain-intrinsic-size |
| rendering-hoist-jsx | LOW | review | Compiler hoists static JSX automatically — manual hoist only for compiler-off projects or generated blobs |
| rendering-hydration-no-flicker | MEDIUM | review | Inline-script prehydration for deterministic boot values (theme/auth-shell) — never for fetched data or user-controlled values; honor CSP |
| rendering-hydration-suppress-warning | LOW-MEDIUM | review | suppressHydrationWarning on the smallest element with intentional server/client text mismatch |
| rendering-resource-hints | HIGH | review | Use react-dom resource-hint APIs in Server Components/layouts for critical resources; avoid hinting everything |
| rendering-script-defer-async | HIGH | review | Mark script tags defer or async (or use next/script with a strategy); type="module" is deferred by default |
| rendering-svg-precision | LOW | review | Run SVGs through SVGO with measured precision; require visual diff for logos/charts/thin strokes |
| rendering-usetransition-loading | LOW | review | useTransition for non-urgent UI updates (search/filter/navigation) — not a replacement for network-lifecycle loading state |
| rerender-defer-reads | MEDIUM | review | Don't subscribe to dynamic state (useSearchParams, etc.) when you only read it inside a callback |
| rerender-dependencies | LOW | review | Depend on the primitive value your Effect actually reads — not the parent object — and don't use this to hide real deps |
| rerender-derived-state | MEDIUM | review | Subscribe to the semantic signal you actually need, not the continuous value behind it |
| rerender-derived-state-no-effect | MEDIUM | review | Derive values during render, not in state synced via Effect |
| rerender-functional-setstate | MEDIUM | lint | Use setState(prev => …) when the new state depends on the current state — primary win is correctness (no stale closure) |
| rerender-lazy-state-init | MEDIUM | review | Pass a function to useState when the initial value requires heavy computation |
| rerender-memo | MEDIUM | review | Profile before manual memoization — prefer React Compiler, pure rendering, and local state |
| rerender-memo-with-default-value | MEDIUM | review | When a memoized component has a non-primitive default prop value, extract the default to a module constant |
| rerender-move-effect-to-event | MEDIUM | review | Side effects triggered by user actions belong in event handlers, not state + Effect |
| rerender-no-inline-components | HIGH | lint | Never define a component inside another component — it remounts on every parent render and destroys state |
| rerender-simple-expression-in-memo | LOW-MEDIUM | review | Don't useMemo a primitive-result expression — the memo overhead exceeds the cost |
| rerender-split-combined-hooks | MEDIUM | review | Split useMemo/useEffect when independent tasks have different dependencies; don't split tightly coupled logic |
| rerender-transitions | MEDIUM | review | Use startTransition for non-urgent state updates that affect rendering — not for imperative bookkeeping |
| rerender-use-deferred-value | MEDIUM | review | useDeferredValue + useMemo for expensive derived renders behind urgent input — fix the algorithm first if hot |
| rerender-use-ref-transient-values | MEDIUM | review | useRef for transient values that don't drive rendering; useState only for values the UI must reflect |
| rich-content-must-use-dynamic-import | HIGH | regex_scan | RichTextEditor and MarkdownRenderer must be imported via next/dynamic in Server Components |
| saved-view-must-be-url-state-or-server-persisted | HIGH | script | SavedView persistence must be 'url' or 'server' — localStorage is forbidden |
| search-frontend-palette-highlight | LOW | review | Search UI must render a Cmd+K SearchPalette posting to the search endpoint and a ResultHighlighter that wraps matches in <mark> |
| server-after-nonblocking | MEDIUM | review | Use after() for best-effort post-response work (logs / analytics / cleanup) — never for critical operations |
| server-auth-actions | CRITICAL | review | Authenticate inside every Server Action — they are public mutation endpoints |
| server-cache-lru | MEDIUM | review | Manual LRU cache for cross-request sharing — fallback when Cache Components is unavailable |
| server-cache-react | MEDIUM | review | Use React.cache() for per-request, in-process deduplication of non-fetch server work |
| server-dedup-props | LOW | review | Don't break RSC prop reference-dedup with sort/filter/map at the Server→Client boundary |
| server-hoist-static-io | HIGH | review | Hoist truly static asset I/O to module scope; never hoist request-, user-, or tenant-scoped data |
| server-parallel-fetching | CRITICAL | review | Parallelize Server Component fetches via composition, Promise.all, or Suspense streaming |
| server-serialization | HIGH | review | Pass minimal client DTOs across the RSC→Client boundary — never whole server entities |
| traceid-propagated-client | HIGH | review | Server Actions must include traceId in error responses so the client can correlate failures with server logs |
| ux-block-uses-design-tokens-and-a11y | HIGH | review | Codified UX blocks must use semantic design tokens (no hardcoded hex/palette), semantic HTML with role/aria for stateful UI, and typed string-literal variant props |
| virtualized-table-when-rowcount-gt-1000 | HIGH | review | DataTable with more than 1000 rows must use VirtualizedTable |
