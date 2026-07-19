// CLEAN — locale-aware formatting via Intl.NumberFormat / Intl.DateTimeFormat,
// composed as a thin route delegating to a feature component (ax/no-god-route
// pattern). The receipt block itself would live under
// templates/L2/blocks/billing-history.tsx in a full build; this fixture
// stays thin because Lane A's route/layer rules don't need the full block.
"use client";

type ReceiptProps = {
  totalAmountMajor: number;
  currency: string;
  paidAt: string; // ISO 8601
  locale: string; // e.g. "ko-KR"
};

export default function ReceiptPage({ totalAmountMajor, currency, paidAt, locale }: ReceiptProps) {
  const amountText = new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
  }).format(totalAmountMajor);

  const dateText = new Intl.DateTimeFormat(locale, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(paidAt));

  return (
    <div>
      <p>Total: {amountText}</p>
      <p>Paid on: {dateText}</p>
    </div>
  );
}
