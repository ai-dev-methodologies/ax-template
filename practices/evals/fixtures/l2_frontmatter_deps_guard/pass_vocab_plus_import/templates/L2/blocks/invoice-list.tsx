/*
---
template_id: L2/blocks/invoice-list
layer: L2
dependencies: [currency-input, button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/payment/]
---
*/
import * as React from 'react'
import { formatCurrencyAmount } from '@/templates/L1/components/currency-input'

// P3-71 regression fixture — 'button' is documentation-only vocabulary (the
// catalog's shadcn-primitive convention: real templates/L1/components/button.tsx
// exists but is intentionally never imported by module specifier). This block
// ALSO imports a different real L1 sibling (currency-input) for its actual
// formatting logic. Pre-fix, the guard's "some OTHER real sibling IS imported
// instead" check flagged 'button' as a stale/renamed dependency the moment ANY
// L1 import existed — a false positive on the guard's own documented invariant
// (generic vocabulary declared without a literal import is legitimate). Post-fix
// (len(declared) != 1 stands down), this must PASS.
export function InvoiceList() {
  return <div><button>Pay</button>{formatCurrencyAmount()}</div>
}
