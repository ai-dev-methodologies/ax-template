import React from 'react';
import type { Metadata } from 'next';
import { Be_Vietnam_Pro, IBM_Plex_Mono } from 'next/font/google';
import { Providers } from '@/components/providers';
import './globals.css';

// fintech-trust font pairing.
//   - Be Vietnam Pro (--font-sans): a tabular-figure grotesk with lined,
//     even-width digits and a calm, slightly humanist letterform — exactly the
//     "tabular-figure grotesk, medium scale-contrast, 15px" the persona spec
//     calls for. Its numerals share a fixed advance, so money columns align
//     without any per-cell hinting; the catalog primitives read var(--font-sans)
//     so they inherit it.
//   - IBM Plex Mono (--font-mono): a numeric monospace used for IDs (payment /
//     order / job UUIDs, idempotency keys) and the statement-line amounts that
//     opt into var(--font-mono). Plex Mono's figures are designed for financial
//     tabulation — a deliberate trust signal.
const grotesk = Be_Vietnam_Pro({
  subsets: ['latin'],
  display: 'swap',
  weight: ['400', '500', '600', '700'],
  variable: '--font-sans',
});
const mono = IBM_Plex_Mono({
  subsets: ['latin'],
  display: 'swap',
  weight: ['400', '500', '600'],
  variable: '--font-mono',
});

export const metadata: Metadata = {
  title: 'ax Pay — 결제 · 정산 콘솔',
  description:
    'fintech-trust 결제/빌링 콘솔 — 결제 · 거래 원장 · 구독/요금제 · 정산 명세서. 멱등 결제, 금액은 자릿수 정렬(tabular)로 정확하게.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={`${grotesk.variable} ${mono.variable}`}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
