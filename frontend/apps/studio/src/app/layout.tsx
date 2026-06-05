import React from 'react';
import type { Metadata } from 'next';
import { Baloo_2, Plus_Jakarta_Sans } from 'next/font/google';
import { Providers } from '@/components/providers';
import './globals.css';

// playful-creator font pairing.
//   - Baloo 2 (--font-display): a CHUNKY ROUNDED display face with soft, friendly
//     terminals and heavy weights — exactly the "chunky rounded display, high
//     scale-contrast" the persona spec calls for. Used for the hero, screen
//     titles, and the wordmark through the .ax-display utility. Its bold weights
//     give the big-vs-small scale contrast the studio surface leans on.
//   - Plus Jakarta Sans (--font-sans): a warm, slightly rounded geometric body
//     face that keeps long copy legible at the persona's 16px base while staying
//     friendly next to the chunky display. The catalog primitives read
//     var(--font-sans) so they inherit it.
const display = Baloo_2({
  subsets: ['latin'],
  display: 'swap',
  weight: ['500', '600', '700', '800'],
  variable: '--font-display',
});
const sans = Plus_Jakarta_Sans({
  subsets: ['latin'],
  display: 'swap',
  weight: ['400', '500', '600', '700'],
  variable: '--font-sans',
});

export const metadata: Metadata = {
  title: 'ax Studio — 크리에이티브 스튜디오',
  description:
    'playful-creator 크리에이티브 스튜디오 — 미디어 업로드 · 갤러리 · 컬렉션 · 반응 · 활동. 작품을 올리고, 컬렉션으로 묶고, 좋아요로 반응하세요.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={`${display.variable} ${sans.variable}`}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
