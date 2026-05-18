// TDD anchor — SP28 fixture: PASS case for no-hardcoded-user-facing-string-in-l4
// This file uses t() for all user-facing strings — no hardcoded Korean literals.
// The rule scanner must detect NO violations and return exit 0.
// Created: 2026-05-18 (within applies_to scope)

'use client'

// In a real app: import { useTranslations } from 'next-intl'
// Here we stub it so the fixture is self-contained.
function useTranslations(_ns: string) {
  return (key: string) => key // stub — real app returns translated string
}

export default function PaymentPage() {
  const t = useTranslations('Payment')
  return (
    <div>
      <h1>{t('title')}</h1>
      <button>{t('submit')}</button>
      <p>{t('amountPrompt')}</p>
    </div>
  )
}
