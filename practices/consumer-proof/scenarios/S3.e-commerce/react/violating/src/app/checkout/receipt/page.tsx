// VIOLATING — raw toLocaleString()/string concatenation instead of Intl.
// Bug this represents: toLocaleString() with no explicit locale/currency
// silently uses the SERVER'S runtime locale (or the browser's, on CSR),
// which drifts between environments and produces wrong grouping/decimal
// separators and currency symbols for a Korean-enterprise checkout receipt.
// Manual string concatenation for the date is even worse — no locale
// awareness at all, and format order (MM/DD vs DD/MM) is hardcoded.
"use client";

type ReceiptProps = {
  totalAmountMajor: number;
  currency: string;
  paidAt: string; // ISO 8601
};

export default function ReceiptPage({ totalAmountMajor, currency, paidAt }: ReceiptProps) {
  const paid = new Date(paidAt);
  // VIOLATION: raw toLocaleString(), no explicit locale/currency options.
  const amountText = totalAmountMajor.toLocaleString() + " " + currency;
  // VIOLATION: manual string concat date formatting, no Intl.DateTimeFormat.
  const dateText = (paid.getMonth() + 1) + "/" + paid.getDate() + "/" + paid.getFullYear();

  return (
    <div>
      <p>Total: {amountText}</p>
      <p>Paid on: {dateText}</p>
    </div>
  );
}
