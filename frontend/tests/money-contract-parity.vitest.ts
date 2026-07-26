import { describe, it, expect } from 'vitest'

// S2.MONEY-QUANTITY.XB — frontend<->backend money-quantity contract parity.
//
// Sibling of frontend/tests/page-envelope-parity.vitest.ts (same pattern: one committed golden, two
// independent consumers — this test + backend/src/test/java/.../payment/MoneyContractParityTest.java
// both read frontend/tests/_fixtures/money-contract.golden.json, which documents exactly what the REAL
// payment HTTP wire (PaymentBodyMapper.toBody — the Map body PaymentController actually returns) and the
// LIVE RefundResponse serialize, verified against Money.java/Payment.java/PaymentService#scale() +
// PaymentBodyMapper.minorOrNull().
//
// P1-68/P1-69 CLOSED (wave-3 money reconciliation): the payment wire is now INTEGER MINOR UNITS in both
// directions — requests (MoneyDeserializer → MoneyWire.resolveMajor) and responses
// (PaymentBodyMapper.minorOrNull / RefundResponse.from → common/Money.toMinorUnits). That is the encoding
// contracts/payment-openapi.yaml#MoneyAmount's integer branch declares and the one parseMinor() below has
// always assumed. Before the fix the wire carried MAJOR-unit decimals (USD 10.99), so parseMinor threw on
// fractional amounts and — worse — silently misread whole-dollar amounts by 100x. Those two mismatch
// locks are gone; the assertions below are now parity assertions.
import {
  parseMinor,
  serializeMinor,
  fractionDigitsFor,
  toMajorUnits,
} from '../../templates/L0/fork-receiver-kit/money'
import golden from './_fixtures/money-contract.golden.json'

describe('money-contract-parity — fractionDigitsFor sanity (money.ts own contract)', () => {
  it('KRW/JPY are 0-fraction-digit; USD is 2', () => {
    expect(fractionDigitsFor('KRW')).toBe(0)
    expect(fractionDigitsFor('USD')).toBe(2)
  })
})

describe('money-contract-parity — KRW payment (scale-0 currency: minor unit == major unit)', () => {
  it('golden.paymentKrw.amount round-trips through parseMinor/toMajorUnits with NO phantom decimals', () => {
    // golden.paymentKrw is what the real payment wire (PaymentBodyMapper.toBody) ACTUALLY serializes for
    // a real ₩12,900 KRW payment (MoneyContractParityTest#paymentBody_krwWholeAmount_serializesAsBareIntegerMatchingGolden).
    expect(golden.paymentKrw.currency).toBe('KRW')
    expect(golden.paymentKrw.amount).toBe(12900)

    const minor = parseMinor(golden.paymentKrw.amount)
    expect(minor).toBe(12900n)
    // The bug this guards: rendering ₩129.00 for a ₩12,900 payment (treating scale-0 KRW like a
    // 2-decimal currency). fractionDigitsFor('KRW') === 0 keeps the rendered form scale-0.
    expect(toMajorUnits(minor, fractionDigitsFor('KRW'))).toBe('12900')
  })

  it('boundary: zero balance (fully refunded KRW payment) round-trips as 0, not "0.00"', () => {
    expect(golden.paymentKrwFullyRefunded.balance).toBe(0)
    const minor = parseMinor(golden.paymentKrwFullyRefunded.balance)
    expect(minor).toBe(0n)
    expect(toMajorUnits(minor, fractionDigitsFor('KRW'))).toBe('0')
  })

  it('boundary: a large KRW amount round-trips without float drift (bigint arithmetic, not Number)', () => {
    expect(golden.paymentKrwLarge.amount).toBe(12345678900)
    const minor = parseMinor(golden.paymentKrwLarge.amount)
    expect(minor).toBe(12345678900n)
    expect(serializeMinor(minor)).toBe(12345678900)
    expect(toMajorUnits(minor, fractionDigitsFor('KRW'))).toBe('12345678900')
  })
})

describe('money-contract-parity — USD payment (P1-68 closed: wire is integer MINOR units)', () => {
  it('golden.paymentUsd.amount is an integer minor-unit count that renders as $10.99', () => {
    // golden.paymentUsd is what the real payment wire (PaymentBodyMapper.toBody) ACTUALLY serializes for
    // a real $10.99 USD payment (MoneyContractParityTest#paymentBody_usd_serializesAsIntegerMinorUnits).
    // Pre-fix this was 10.99 and parseMinor threw RangeError; the contract and the emitter now agree.
    expect(golden.paymentUsd.currency).toBe('USD')
    expect(golden.paymentUsd.amount).toBe(1099)

    const minor = parseMinor(golden.paymentUsd.amount)
    expect(minor).toBe(1099n)
    expect(toMajorUnits(minor, fractionDigitsFor('USD'))).toBe('10.99')
  })

  it('capturedAmount/balance carry the same minor-unit encoding as amount', () => {
    expect(parseMinor(golden.paymentUsd.capturedAmount)).toBe(1099n)
    expect(parseMinor(golden.paymentUsd.balance)).toBe(1099n)
  })

  it('a whole-dollar USD amount is no longer misread 100x too small', () => {
    // The dangerous half of the old mismatch: a $100.00 payment used to serialize as the JSON number
    // 100.00, which JSON.parse yields as the integer 100 — so parseMinor did NOT throw and silently
    // returned 100n ($1.00), 100x too small. The wire now carries 10000 for $100.00, and parseMinor's
    // own contract ("the wire is already integer minor units") finally holds.
    const wholeDollarUsdWireValue = 10000 // what the emitter serializes for a $100.00 payment
    expect(toMajorUnits(parseMinor(wholeDollarUsdWireValue), fractionDigitsFor('USD'))).toBe('100.00')
  })
})

describe('money-contract-parity — refund (same unified encoding as payments)', () => {
  it('golden.refundUsd.amount is a positive integer minor-unit count rendering as $4.99', () => {
    // RefundResponse.amount is ALWAYS positive per Refund.java (a refund is its own entity, never a
    // negative delta applied to Payment.amount) — MoneyContractParityTest#refundResponse_usd_isAlwaysPositive_matchesGolden.
    expect(golden.refundUsd.currency).toBe('USD')
    expect(golden.refundUsd.amount).toBe(499)

    const minor = parseMinor(golden.refundUsd.amount)
    expect(minor).toBe(499n)
    expect(toMajorUnits(minor, fractionDigitsFor('USD'))).toBe('4.99')
  })
})

describe('money-contract-parity — synthetic BILLING-domain minor-unit value (negative-sign isolation check)', () => {
  it('parseMinor correctly handles a negative integer minor-unit value', () => {
    // golden.syntheticBillingMinorNegative is explicitly NOT a payment-domain BE emission (see its
    // _note) — no payment amount is ever negative, so this synthetic value is the only fixture that
    // exercises parseMinor's negative-sign handling.
    expect(golden.syntheticBillingMinorNegative.minorAmount).toBe(-500)
    const minor = parseMinor(golden.syntheticBillingMinorNegative.minorAmount)
    expect(minor).toBe(-500n)
    expect(toMajorUnits(minor, fractionDigitsFor('USD'))).toBe('-5.00')
  })
})
