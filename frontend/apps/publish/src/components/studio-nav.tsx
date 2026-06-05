'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LibraryBig, PenLine, Search, Tags } from 'lucide-react';
import { cn } from '@ax/ui';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
}

const NAV: NavItem[] = [
  { href: '/', label: '라이브러리', icon: LibraryBig },
  { href: '/editor', label: '새 글', icon: PenLine },
  { href: '/tags', label: '태그', icon: Tags },
  { href: '/search', label: '검색', icon: Search },
];

function isActive(pathname: string, href: string): boolean {
  if (href === '/') return pathname === '/';
  return pathname === href || pathname.startsWith(`${href}/`);
}

interface StudioNavProps {
  /** 'rail' = vertical sidebar (desktop), 'bar' = horizontal bottom bar (mobile) */
  variant?: 'rail' | 'bar';
}

/**
 * Persona-themed navigation. The editorial-luxury persona favors sharp-cornered
 * items with a left ink rule on the active entry (a magazine-section marker) —
 * no rounded pills, no float. Renders as a vertical rail on desktop and a sticky
 * bottom bar on mobile.
 */
export function StudioNav({ variant = 'rail' }: StudioNavProps) {
  const pathname = usePathname();
  const isBar = variant === 'bar';

  return (
    <nav
      aria-label="주요 메뉴"
      className={cn(isBar ? 'grid grid-cols-4 gap-1' : 'flex flex-col')}
    >
      {NAV.map(({ href, label, icon: Icon }) => {
        const active = isActive(pathname, href);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? 'page' : undefined}
            className={cn(
              'group relative flex items-center font-medium transition-colors duration-200 ease-out',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
              isBar
                ? 'flex-col justify-center gap-1 px-2 py-2 text-xs'
                : 'gap-3 border-l-2 px-4 py-3 text-sm',
              active
                ? isBar
                  ? 'text-foreground'
                  : 'border-foreground bg-secondary/60 text-foreground'
                : isBar
                  ? 'text-muted-foreground hover:text-foreground'
                  : 'border-transparent text-muted-foreground hover:border-border hover:bg-secondary/40 hover:text-foreground',
            )}
          >
            <Icon aria-hidden className="h-5 w-5 shrink-0" />
            <span className={cn(isBar ? '' : 'uppercase tracking-[0.06em]')}>{label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
