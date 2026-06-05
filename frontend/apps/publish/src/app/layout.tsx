import React from 'react';
import type { Metadata } from 'next';
import { Playfair_Display, Source_Serif_4 } from 'next/font/google';
import { Providers } from '@/components/providers';
import './globals.css';

// editorial-luxury font pairing.
//   - Playfair Display (--font-display): a high-contrast didone serif with
//     dramatic thick/thin stroke contrast — the masthead + headlines, set at
//     EXTREME scale (the persona's "high-contrast serif display, extreme
//     scale-contrast").
//   - Source Serif 4 (--font-sans slot): a refined reading serif for body +
//     UI text. The shell uses a serif everywhere — the magazine feel — so the
//     catalog primitives (which read var(--font-sans)) inherit the body serif.
const display = Playfair_Display({
  subsets: ['latin'],
  display: 'swap',
  weight: ['400', '600', '700', '800', '900'],
  style: ['normal', 'italic'],
  variable: '--font-display',
});
const body = Source_Serif_4({
  subsets: ['latin'],
  display: 'swap',
  weight: ['400', '500', '600', '700'],
  style: ['normal', 'italic'],
  variable: '--font-sans',
});

export const metadata: Metadata = {
  title: 'ax Press — 에디토리얼 스튜디오',
  description: 'editorial-luxury 콘텐츠 발행 CMS — 라이브러리 · 에디터 · 태그 · 검색',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={`${display.variable} ${body.variable}`}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
