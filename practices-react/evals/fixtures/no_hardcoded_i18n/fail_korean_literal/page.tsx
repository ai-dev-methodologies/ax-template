// TDD anchor — SP28 fixture: FAIL case for no-hardcoded-user-facing-string-in-l4
// This file is INTENTIONALLY WRONG — it contains hardcoded Korean literals in JSX.
// The rule scanner must detect this and return a non-zero exit code.
// Path is under practices-react/evals/fixtures/ (SP28-scoped, not existing L4).
// Created: 2026-05-18 (within applies_to scope)

export default function PaymentPage() {
  return (
    <div>
      <h1>결제</h1>
      <button>결제하기</button>
      <p>금액을 입력해 주세요.</p>
    </div>
  )
}
