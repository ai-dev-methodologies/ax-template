'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Bell, Bookmark, Home, Search } from 'lucide-react';
import { cn } from '@ax/ui';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
}

const NAV: NavItem[] = [
  { href: '/', label: '피드', icon: Home },
  { href: '/search', label: '검색', icon: Search },
  { href: '/favorites', label: '즐겨찾기', icon: Bookmark },
  { href: '/notifications', label: '알림', icon: Bell },
];

function isActive(pathname: string, href: string): boolean {
  if (href === '/') return pathname === '/';
  return pathname === href || pathname.startsWith(`${href}/`);
}

interface ConsumerNavProps {
  /** unread notification count, surfaced as a badge on the 알림 item */
  unreadCount?: number;
  /** 'rail' = vertical sidebar (desktop), 'bar' = horizontal bottom bar (mobile) */
  variant?: 'rail' | 'bar';
}

/**
 * Persona-themed navigation. The consumer-delight persona favors a friendly,
 * rounded pill with a springy hover lift (motion budget: hover spring). Renders
 * as a vertical rail on desktop and a sticky bottom bar on mobile.
 */
export function ConsumerNav({ unreadCount = 0, variant = 'rail' }: ConsumerNavProps) {
  const pathname = usePathname();
  const isBar = variant === 'bar';

  return (
    <nav
      aria-label="주요 메뉴"
      className={cn(
        isBar ? 'grid grid-cols-4 gap-1' : 'flex flex-col gap-1',
      )}
    >
      {NAV.map(({ href, label, icon: Icon }) => {
        const active = isActive(pathname, href);
        const showBadge = href === '/notifications' && unreadCount > 0;
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? 'page' : undefined}
            className={cn(
              'group relative flex items-center rounded-[var(--radius)] font-medium transition-transform duration-200 ease-out',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
              isBar
                ? 'flex-col justify-center gap-1 px-2 py-2 text-xs'
                : 'gap-3 px-4 py-2.5 text-sm hover:-translate-y-0.5 motion-reduce:hover:translate-y-0',
              active
                ? 'bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]'
                : 'text-muted-foreground hover:bg-secondary/70 hover:text-foreground',
            )}
          >
            <span className="relative inline-flex">
              <Icon aria-hidden className={cn(isBar ? 'h-5 w-5' : 'h-5 w-5 shrink-0')} />
              {showBadge ? (
                <span
                  aria-hidden="true"
                  className="absolute -right-2 -top-1.5 grid h-4 min-w-4 place-items-center rounded-full bg-[var(--ax-status-danger-fg)] px-1 text-[0.6rem] font-bold leading-none text-[var(--ax-status-danger-bg)]"
                >
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              ) : null}
            </span>
            <span>{label}</span>
            {showBadge ? (
              <span className="sr-only">{`(읽지 않은 알림 ${unreadCount}개)`}</span>
            ) : null}
          </Link>
        );
      })}
    </nav>
  );
}
