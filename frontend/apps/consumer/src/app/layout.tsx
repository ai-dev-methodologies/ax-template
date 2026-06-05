import React from 'react';
import type { Metadata } from 'next';
import { Plus_Jakarta_Sans, Quicksand } from 'next/font/google';
import { Providers } from '@/components/providers';
import './globals.css';

// consumer-delight font pairing: Plus Jakarta Sans (rounded humanist body, the
// persona's "rounded humanist, 16px body") + Quicksand for the playful display
// wordmark — friendlier and more characterful than the operator's mono stack.
const sans = Plus_Jakarta_Sans({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-sans',
});
const display = Quicksand({
  subsets: ['latin'],
  display: 'swap',
  weight: ['500', '600', '700'],
  variable: '--font-display',
});

export const metadata: Metadata = {
  title: 'ax moment — 우리들의 피드',
  description: 'consumer-delight 소셜 피드 — 활동 · 댓글 · 즐겨찾기 · 알림 · 검색',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={`${sans.variable} ${display.variable}`}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
