import React from 'react';
import type { Metadata } from 'next';
import { Archivo, JetBrains_Mono } from 'next/font/google';
import { Providers } from '@/components/providers';
import './globals.css';

// developer-tool font pairing.
//   - JetBrains Mono (--font-mono): a purpose-built coding monospace with a tall
//     x-height and unambiguous glyphs — every CodeSnippet HTTP exchange, ETag,
//     token, header, and DataGrid numeric column renders in it. The persona's
//     signature is "mono code surfaces".
//   - Archivo (--font-sans slot): a grotesk with a slightly condensed, technical
//     character for prose + UI chrome, set at a dense 14px base (globals.css
//     .ax-devtool). The catalog primitives read var(--font-sans), so they
//     inherit the grotesk; code surfaces opt into var(--font-mono).
const grotesk = Archivo({
  subsets: ['latin'],
  display: 'swap',
  weight: ['400', '500', '600', '700'],
  variable: '--font-sans',
});
const mono = JetBrains_Mono({
  subsets: ['latin'],
  display: 'swap',
  weight: ['400', '500', '600', '700'],
  variable: '--font-mono',
});

export const metadata: Metadata = {
  title: 'ax Console — 개발자 콘솔',
  description:
    'developer-tool API 플랫폼 — API 키 · 웹훅 · 멱등성 · 문제 응답 · 낙관적 잠금 · 요청 검증. 모든 동작의 실제 HTTP 요청/응답을 보여줍니다.',
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
