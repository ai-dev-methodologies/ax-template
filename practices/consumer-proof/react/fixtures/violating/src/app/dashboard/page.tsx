// VIOLATING — ax/no-god-route
// A Next.js App Router "use client" route file that has grown past the line
// threshold (> 100 lines) by absorbing form state + business logic + inline UI
// that belongs in a @/features/<f> container. Written with immutable state
// updates on purpose so ONLY the god-route size signal fires.
'use client'
import { useState } from 'react'

type Line = { id: number; sku: string; qty: number; price: number }

export default function DashboardPage() {
  const [lines, setLines] = useState<Line[]>([])
  const [sku, setSku] = useState('')
  const [qty, setQty] = useState(1)
  const [price, setPrice] = useState(0)
  const [note, setNote] = useState('')
  const [discountCode, setDiscountCode] = useState('')
  const [taxRegion, setTaxRegion] = useState('KR')
  const [submitting, setSubmitting] = useState(false)
  const [errors, setErrors] = useState<string[]>([])

  function validate(): string[] {
    const next: string[] = []
    if (sku.trim().length === 0) next.push('SKU is required')
    if (qty <= 0) next.push('Quantity must be positive')
    if (price < 0) next.push('Price cannot be negative')
    if (taxRegion.length !== 2) next.push('Tax region must be a 2-letter code')
    return next
  }

  function addLine() {
    const problems = validate()
    if (problems.length > 0) {
      setErrors(() => problems)
      return
    }
    setLines((prev) => [
      ...prev,
      { id: prev.length + 1, sku, qty, price },
    ])
    setSku(() => '')
    setQty(() => 1)
    setPrice(() => 0)
    setErrors(() => [])
  }

  function removeLine(id: number) {
    setLines((prev) => prev.filter((l) => l.id !== id))
  }

  function subtotal(): number {
    return lines.reduce((sum, l) => sum + l.qty * l.price, 0)
  }

  function discount(): number {
    if (discountCode === 'SAVE10') return subtotal() * 0.1
    if (discountCode === 'SAVE20') return subtotal() * 0.2
    return 0
  }

  function taxRate(): number {
    if (taxRegion === 'KR') return 0.1
    if (taxRegion === 'US') return 0.07
    if (taxRegion === 'JP') return 0.08
    return 0
  }

  function tax(): number {
    return (subtotal() - discount()) * taxRate()
  }

  function total(): number {
    return subtotal() - discount() + tax()
  }

  async function submit() {
    const problems = validate()
    if (problems.length > 0) {
      setErrors(() => problems)
      return
    }
    setSubmitting(() => true)
    try {
      await fetch('/api/orders', {
        method: 'POST',
        body: JSON.stringify({ lines, note, discountCode, taxRegion }),
      })
    } finally {
      setSubmitting(() => false)
    }
  }

  return (
    <main>
      <h1>Order Dashboard</h1>
      <section>
        <input value={sku} onChange={(e) => setSku(() => e.target.value)} />
        <input
          type="number"
          value={qty}
          onChange={(e) => setQty(() => Number(e.target.value))}
        />
        <input
          type="number"
          value={price}
          onChange={(e) => setPrice(() => Number(e.target.value))}
        />
        <button onClick={addLine}>Add line</button>
      </section>
      <section>
        <textarea value={note} onChange={(e) => setNote(() => e.target.value)} />
        <input
          value={discountCode}
          onChange={(e) => setDiscountCode(() => e.target.value)}
        />
        <select
          value={taxRegion}
          onChange={(e) => setTaxRegion(() => e.target.value)}
        >
          <option value="KR">KR</option>
          <option value="US">US</option>
          <option value="JP">JP</option>
        </select>
      </section>
      <ul>
        {lines.map((l) => (
          <li key={l.id}>
            {l.sku} x {l.qty} @ {l.price}
            <button onClick={() => removeLine(l.id)}>remove</button>
          </li>
        ))}
      </ul>
      <ul>
        {errors.map((err) => (
          <li key={err}>{err}</li>
        ))}
      </ul>
      <footer>
        <div>Subtotal: {subtotal()}</div>
        <div>Discount: {discount()}</div>
        <div>Tax: {tax()}</div>
        <div>Total: {total()}</div>
        <button disabled={submitting} onClick={submit}>
          Submit order
        </button>
      </footer>
    </main>
  )
}
