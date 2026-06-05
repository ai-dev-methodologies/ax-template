'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  CreditCard,
  LayoutDashboard,
  ReceiptText,
  Repeat,
  ScrollText,
} from 'lucide-react';
import { cn } from '@ax/ui';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
}

const NAV: NavItem[] = [
  { href: '/', label: '개요', icon: LayoutDashboard },
  { href: '/checkout', label: '결제하기', icon: CreditCard },
  { href: '/transactions', label: '거래 원장', icon: ReceiptText },
  { href: '/subscriptions', label: '구독·요금제', icon: Repeat },
  { href: '/statements', label: '정산 명세서', icon: ScrollText },
];

function isActive(pathname: string, href: string): boolean {
  if (href === '/') return pathname === '/';
  return pathname === href || pathname.startsWith(`${href}/`);
}

interface LedgerNavProps {
  /** 'rail' = vertical sidebar (desktop), 'bar' = horizontal bottom bar (mobile) */
  variant?: 'rail' | 'bar';
}

/**
 * Persona-themed navigation. The fintech-trust persona favors calm,
 * 8px-cornered items with a soft accent-tinted active state (a settled "you are
 * here" surface, not a loud pill or float). Renders as a vertical rail on
 * desktop and a sticky bottom bar (5 entries) on mobile. Conservative
 * color-only transitions.
 */
export function LedgerNav({ variant = 'rail' }: LedgerNavProps) {
  const pathname = usePathname();
  const isBar = variant === 'bar';

  return (
    <nav
      aria-label="주요 메뉴"
      className={cn(isBar ? 'flex justify-between gap-1' : 'flex flex-col gap-1')}
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
                ? 'min-w-0 flex-1 flex-col justify-center gap-1 rounded-[var(--radius)] px-1.5 py-2 text-[0.65rem]'
                : 'gap-3 rounded-[var(--radius)] px-3 py-2 text-[0.9rem]',
              active
                ? isBar
                  ? 'text-[var(--ax-status-accent-fg)]'
                  : 'bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]'
                : isBar
                  ? 'text-muted-foreground hover:text-foreground'
                  : 'text-muted-foreground hover:bg-secondary hover:text-foreground',
            )}
          >
            <Icon aria-hidden className="h-[1.1rem] w-[1.1rem] shrink-0" />
            <span className={cn(isBar ? 'truncate' : 'tracking-tight')}>{label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
