import React from 'react';
import type { Metadata } from 'next';
import { Plus_Jakarta_Sans, JetBrains_Mono } from 'next/font/google';
import { Providers } from '@/components/providers';
import './globals.css';

// Shared font pairing (matches the ax design system): Plus Jakarta Sans for
// body/display + JetBrains Mono for the wordmark + dense numeric/labels.
const sans = Plus_Jakarta_Sans({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-sans',
});
const mono = JetBrains_Mono({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-mono',
});

export const metadata: Metadata = {
  title: 'ax operator — 운영 콘솔',
  description: 'enterprise-operator 운영 콘솔 — 감사 · 결재 · 기능 플래그 · 세션 · 리포트',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={`${sans.variable} ${mono.variable}`}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
