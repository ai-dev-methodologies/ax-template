import { describe, it, expect } from 'vitest'

// S2.MONEY-QUANTITY.XB — frontend<->backend money-quantity contract parity.
//
// Sibling of frontend/tests/page-envelope-parity.vitest.ts (same pattern: one committed golden, two
// independent consumers — this test + backend/src/test/java/.../payment/MoneyContractParityTest.java
// both read frontend/tests/_fixtures/money-contract.golden.json, which documents exactly what the REAL
// payment HTTP wire (PaymentBodyMapper.toBody — the Map body PaymentController actually returns) and the
// LIVE RefundResponse serialize, verified against Money.java/Payment.java/PaymentService#scale() +
// PaymentBodyMapper.canonicalize(). (The earlier golden was derived from PaymentResponse, a dead record
// no endpoint returns — wave-2 finding-3 repointed the payment legs to the real emitter.)
//
// CONFIRMED FINDING (P1 candidate — see the session report for the full evidence chain, and the
// class Javadoc on MoneyContractParityTest for the BE-side half): Payment/Refund amount is a
// MAJOR-unit BigDecimal (per common/Money.java's own documented "Payment / PG-edge layer" convention),
// scaled to the currency's ISO-4217 minor-unit count. For KRW (scale 0) that number happens to equal
// the minor-unit count too, so parseMinor() (which assumes the wire is ALREADY integer minor units)
// accidentally "works". For USD (scale 2) it does not — the wire carries a decimal point. This file
// locks BOTH the working case (KRW) and the two flavors of the confirmed mismatch (USD: throws on
// fractional amounts, silently misreads by 100x on whole-dollar amounts) as standing assertions rather
// than papering over them.
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

describe('money-contract-parity — KRW payment (scale-0 coincidence: parity happens to hold)', () => {
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

describe('money-contract-parity — USD payment (CONFIRMED MISMATCH: wire is MAJOR units, not minor)', () => {
  it('throws: a fractional USD amount (e.g. $10.99) is not an integer, so parseMinor rejects it', () => {
    // golden.paymentUsd is what the real payment wire (PaymentBodyMapper.toBody) ACTUALLY serializes for
    // a real $10.99 USD payment (MoneyContractParityTest#paymentBody_usd_serializesWithDecimalPoint_notMinorUnits).
    // contracts/payment-openapi.yaml's MoneyAmount schema AND this module's own parseMinor() doc both
    // assume the wire is ALREADY an integer minor-unit count (e.g. 1099 for $10.99). It is not — BE
    // emits 10.99, a MAJOR-unit decimal. This assertion locks the throwing half of the mismatch: it
    // must keep throwing until the BE contract or serialization changes.
    expect(golden.paymentUsd.currency).toBe('USD')
    expect(golden.paymentUsd.amount).toBe(10.99)
    expect(() => parseMinor(golden.paymentUsd.amount)).toThrow(RangeError)
  })

  it('silently wrong (does NOT throw): a whole-dollar USD amount is misread 100x too small', () => {
    // A $100.00 payment persists as BigDecimal("100.00") and Jackson serializes it as the JSON number
    // 100.00 — but JSON.parse("100.00") produces the JS number 100, and Number.isInteger(100) is true
    // (trailing zeros carry no weight once parsed), so parseMinor does NOT throw here. What it returns
    // (100n minor units = $1.00 once rendered) is 100x smaller than the real amount (100 major units =
    // 10000 minor units) — the exact "silent 100x" bug common/Money.java's own Javadoc names as
    // unacceptable. This is the more dangerous half of the mismatch: nothing signals the corruption.
    const wholeDollarUsdWireValue = 100 // what JSON.parse('{"amount":100.00}') yields
    const misread = parseMinor(wholeDollarUsdWireValue)
    expect(misread).toBe(100n)
    expect(misread).not.toBe(10000n) // what it WOULD be if 100 correctly meant "100 minor units"
    expect(toMajorUnits(misread, fractionDigitsFor('USD'))).toBe('1.00') // wrong: should render $100.00
  })
})

describe('money-contract-parity — refund (same mismatch class as payments)', () => {
  it('golden.refundUsd.amount is a positive MAJOR-unit USD decimal; parseMinor rejects it', () => {
    // RefundResponse.amount is ALWAYS positive per Refund.java (a refund is its own entity, never a
    // negative delta applied to Payment.amount) — MoneyContractParityTest#refundResponse_usd_isAlwaysPositive_matchesGolden.
    expect(golden.refundUsd.currency).toBe('USD')
    expect(golden.refundUsd.amount).toBe(4.99)
    expect(() => parseMinor(golden.refundUsd.amount)).toThrow(RangeError)
  })
})

describe('money-contract-parity — synthetic BILLING-domain minor-unit value (isolation check, not a BE emission)', () => {
  it('parseMinor DOES correctly handle a negative integer minor-unit value on its native (BILLING) shape', () => {
    // golden.syntheticBillingMinorNegative is explicitly NOT a payment-domain BE emission (see its
    // _note) — it exercises parseMinor's negative-sign handling on the shape money.ts's own doc says
    // it targets (an already-integer minor-unit count), independent of the PAYMENT-domain mismatch above.
    expect(golden.syntheticBillingMinorNegative.minorAmount).toBe(-500)
    const minor = parseMinor(golden.syntheticBillingMinorNegative.minorAmount)
    expect(minor).toBe(-500n)
    expect(toMajorUnits(minor, fractionDigitsFor('USD'))).toBe('-5.00')
  })
})
