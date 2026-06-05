'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  ClipboardCheck,
  Download,
  LayoutGrid,
  MonitorSmartphone,
  ScrollText,
  ToggleRight,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
}

const NAV: NavItem[] = [
  { href: '/enterprise', label: '개요', icon: LayoutGrid },
  { href: '/enterprise/audit-logs', label: '감사 로그', icon: ScrollText },
  { href: '/enterprise/approvals', label: '결재함', icon: ClipboardCheck },
  { href: '/enterprise/feature-flags', label: '기능 플래그', icon: ToggleRight },
  { href: '/enterprise/sessions', label: '세션', icon: MonitorSmartphone },
  { href: '/enterprise/exports', label: '리포트 추출', icon: Download },
];

function isActive(pathname: string, href: string): boolean {
  if (href === '/enterprise') return pathname === '/enterprise';
  return pathname === href || pathname.startsWith(`${href}/`);
}

/**
 * Persona-themed navigation rail for the operations console. The active item
 * uses a flat token fill (no glow) per the enterprise-operator motion budget.
 */
export function ConsoleNav() {
  const pathname = usePathname();
  return (
    <nav aria-label="콘솔 메뉴" className="flex flex-col gap-0.5">
      {NAV.map(({ href, label, icon: Icon }) => {
        const active = isActive(pathname, href);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? 'page' : undefined}
            className={cn(
              'flex items-center gap-2.5 rounded-[var(--radius)] px-3 py-2 text-sm font-medium transition-colors duration-150 motion-reduce:transition-none',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
              active
                ? 'bg-secondary text-secondary-foreground'
                : 'text-muted-foreground hover:bg-secondary/60 hover:text-foreground',
            )}
          >
            <Icon aria-hidden className={cn('h-4 w-4 shrink-0', active && 'text-foreground')} />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
