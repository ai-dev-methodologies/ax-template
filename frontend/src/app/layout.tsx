import React from 'react';
import type { Metadata } from 'next';
import { Providers } from '../components/providers';
import './globals.css';

export const metadata: Metadata = {
  title: 'ax-template',
  description: 'AI-friendly full-stack template — React + Spring Boot',
};

interface RootLayoutProps {
  children: React.ReactNode;
}

export default function RootLayout({ children }: RootLayoutProps) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          {children}
        </Providers>
      </body>
    </html>
  );
}
