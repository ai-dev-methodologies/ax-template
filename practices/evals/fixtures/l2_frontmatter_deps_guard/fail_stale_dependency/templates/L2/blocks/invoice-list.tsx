/*
---
template_id: L2/blocks/invoice-list
layer: L2
dependencies: [currency-formatter]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/payment/]
---
*/
import * as React from 'react'
import { formatCurrencyAmount } from '@/templates/L1/components/currency-input'

export function InvoiceList() {
  return <div>{formatCurrencyAmount()}</div>
}
