import { describe, it, expect, vi, afterEach } from 'vitest'
import { render } from '@testing-library/react'
import PaymentCheckoutForm from '../../templates/L2/blocks/payment-checkout-form'

// CANARY-007 closure (S2.TIME-LOCALE.FE): the wave-1 rule + shell guard
// (practices-react/rules/locale-aware-number-date-format.md) block MANUAL
// locale-formatting patterns (toFixed/currency-concat/manual-date-join) in
// frontend/src, but nothing previously proved that a real, SHIPPED component
// actually renders CORRECTLY per locale via Intl. This file closes that gap
// against templates/L2/blocks/payment-checkout-form.tsx — the one L2 block
// with real Intl.NumberFormat usage (see its `formatAmount` helper).
//
// Note: payment-checkout-form.tsx is not (yet) re-exported through the
// @ax/blocks package (packages/blocks/src/ has no payment-checkout-form.tsx,
// and the "@ax/blocks/*" alias in both vitest.config.ts and tsconfig.json
// resolves only into packages/blocks/src/*). It is imported here the same
// way other repo-root `templates/*` sources already reach frontend/tests/
// (see fmw2-primitives.vitest.ts / fmw4-primitives.vitest.ts / parse-error-
// denylist.vitest.ts, all of which import `../../templates/L0/...` directly).
//
// Locale is NOT a prop on this component — `formatAmount` calls
// `new Intl.NumberFormat(undefined, opts)`, i.e. it reads the AMBIENT
// runtime default locale. To exercise ko-KR and en-US deterministically
// (independent of whatever locale the host/CI machine happens to default
// to), we spy on the global Intl.NumberFormat constructor and substitute a
// pinned locale whenever the component calls it with `undefined`. The real
// Intl.NumberFormat still performs all actual formatting — only the locale
// selection is pinned, so this proves the component's OWN Intl usage, not a
// fake formatter.

const RealNumberFormat = Intl.NumberFormat

function pinLocale(locale: string) {
  // NOTE: must be a `function` expression, not an arrow function — vitest's
  // spy inspects `new.target` to support being invoked with `new` (which
  // `formatAmount` does: `new Intl.NumberFormat(...)`), and refuses to
  // construct through an arrow-function implementation ("is not a
  // constructor"), confirmed empirically while building this test.
  return vi.spyOn(Intl, 'NumberFormat').mockImplementation(function (
    loc?: string | string[],
    opts?: Intl.NumberFormatOptions,
  ) {
    return new RealNumberFormat(loc ?? locale, opts) as unknown as Intl.NumberFormat
  })
}

function renderedAmountText(container: HTMLElement): string | null {
  // The "Amount due" display node: <p className="text-2xl font-bold tabular-nums">{formatAmount(...)}</p>
  return container.querySelector('p.text-2xl')?.textContent ?? null
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('payment-checkout-form: shipped Intl-based amount formatting is correct per locale (CANARY-007 closure)', () => {
  it('ko-KR: the rendered KRW amount equals the Intl.NumberFormat(ko-KR) reference (computed in-test, not hardcoded)', () => {
    pinLocale('ko-KR')
    const { container } = render(
      <PaymentCheckoutForm amount={109900} currency="KRW" onSubmit={() => {}} />,
    )

    // `amount` is integer MINOR units and KRW is a ZERO-decimal currency, so 109900 minor units
    // ARE ₩109,900 — no division, no fraction digits. The pre-fix reference here mirrored the
    // block's own `/ 100` + `minimumFractionDigits: 2` and therefore asserted ₩1,099.00: it
    // recomputed the same wrong formula, so it could never fail on the 100x KRW defect
    // (BACKLOG P2-27). The reference is now derived from the currency's ISO 4217 minor-unit width.
    const reference = new RealNumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(109900)

    expect(renderedAmountText(container)).toBe(reference)
  })

  it('en-US: the rendered USD amount equals the Intl.NumberFormat(en-US) reference (computed in-test, not hardcoded)', () => {
    pinLocale('en-US')
    const { container } = render(
      <PaymentCheckoutForm amount={1099} currency="USD" onSubmit={() => {}} />,
    )

    const reference = new RealNumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
    }).format(1099 / 100)

    expect(renderedAmountText(container)).toBe(reference)
  })

  it('ko-KR and en-US render DIFFERENT output for the identical amount/currency (proves the locale actually drives formatting)', () => {
    // USD differs visibly across these two locales (symbol prefix "US$" vs
    // "$" — see Intl.NumberFormat behavior); KRW happens to render
    // identically under both (₩ symbol + comma grouping in both locales),
    // so USD is the discriminating case here.
    pinLocale('ko-KR')
    const ko = render(<PaymentCheckoutForm amount={1099} currency="USD" onSubmit={() => {}} />)
    const koText = renderedAmountText(ko.container)
    ko.unmount()

    pinLocale('en-US')
    const us = render(<PaymentCheckoutForm amount={1099} currency="USD" onSubmit={() => {}} />)
    const usText = renderedAmountText(us.container)
    us.unmount()

    expect(koText).not.toBeNull()
    expect(usText).not.toBeNull()
    expect(koText).not.toBe(usText)

    // and each still equals its own from-scratch Intl reference
    expect(koText).toBe(
      new RealNumberFormat('ko-KR', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(
        1099 / 100,
      ),
    )
    expect(usText).toBe(
      new RealNumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(
        1099 / 100,
      ),
    )
  })

  // No date is rendered by payment-checkout-form.tsx (confirmed by reading
  // the source — only formatAmount()/Intl.NumberFormat is used, there is no
  // Intl.DateTimeFormat call and no date prop). The task's conditional
  // "assert the date renders locale-formatted, if the component renders
  // one" therefore does not apply to this component; no date assertion is
  // included here for that reason (not an oversight).
})
