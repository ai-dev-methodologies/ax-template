'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  FileWarning,
  GitCompareArrows,
  KeyRound,
  Repeat2,
  ShieldCheck,
  Webhook,
} from 'lucide-react';
import { cn } from '@ax/ui';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
}

const NAV: NavItem[] = [
  { href: '/keys', label: 'API 키', icon: KeyRound },
  { href: '/webhooks', label: '웹훅', icon: Webhook },
  { href: '/idempotency', label: '멱등성', icon: Repeat2 },
  { href: '/problems', label: '문제 응답', icon: FileWarning },
  { href: '/optlock', label: '낙관적 잠금', icon: GitCompareArrows },
  { href: '/validation', label: '요청 검증', icon: ShieldCheck },
];

function isActive(pathname: string, href: string): boolean {
  return pathname === href || pathname.startsWith(`${href}/`);
}

interface DevtoolNavProps {
  /** 'rail' = vertical sidebar (desktop), 'bar' = horizontal bottom bar (mobile) */
  variant?: 'rail' | 'bar';
}

/**
 * Persona-themed navigation. The developer-tool persona favors tight-cornered
 * items with a left cyan rule on the active entry (an IDE file-tree marker) — no
 * float, no rounded pills. Renders as a vertical rail on desktop and a sticky
 * bottom bar (scrollable, 6 entries) on mobile.
 */
export function DevtoolNav({ variant = 'rail' }: DevtoolNavProps) {
  const pathname = usePathname();
  const isBar = variant === 'bar';

  return (
    <nav
      aria-label="주요 메뉴"
      className={cn(isBar ? 'flex justify-between gap-1' : 'flex flex-col')}
    >
      {NAV.map(({ href, label, icon: Icon }) => {
        const active = isActive(pathname, href);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? 'page' : undefined}
            className={cn(
              'group relative flex items-center font-medium transition-colors duration-150 ease-out',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
              isBar
                ? 'min-w-0 flex-1 flex-col justify-center gap-1 rounded px-1.5 py-2 text-[0.65rem]'
                : 'gap-3 rounded border-l-2 px-3 py-2 text-[0.85rem]',
              active
                ? isBar
                  ? 'text-[var(--ax-status-accent-fg)]'
                  : 'border-[var(--ax-status-accent-fg)] bg-secondary/70 text-foreground'
                : isBar
                  ? 'text-muted-foreground hover:text-foreground'
                  : 'border-transparent text-muted-foreground hover:border-border hover:bg-secondary/40 hover:text-foreground',
            )}
          >
            <Icon aria-hidden className="h-[1.05rem] w-[1.05rem] shrink-0" />
            <span className={cn(isBar ? 'truncate' : 'font-mono lowercase tracking-tight')}>
              {label}
            </span>
          </Link>
        );
      })}
    </nav>
  );
}
