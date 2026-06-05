'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Activity, FolderHeart, LayoutGrid, Sparkles, Upload } from 'lucide-react';
import { cn } from '@ax/ui';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
}

const NAV: NavItem[] = [
  { href: '/', label: '스튜디오', icon: Sparkles },
  { href: '/gallery', label: '갤러리', icon: LayoutGrid },
  { href: '/upload', label: '업로드', icon: Upload },
  { href: '/collections', label: '컬렉션', icon: FolderHeart },
  { href: '/activity', label: '활동', icon: Activity },
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
 * Persona-themed navigation. The playful-creator persona favors a very rounded
 * (24px), high-saturation pill with a springy hover float (motion budget:
 * cinematic). Active = a vivid accent-tinted surface. Renders as a vertical rail
 * on desktop and a sticky bottom bar on mobile. The float lift is compositor-
 * friendly and disabled under prefers-reduced-motion via the .ax-float reduce
 * rule in globals.css.
 */
export function StudioNav({ variant = 'rail' }: StudioNavProps) {
  const pathname = usePathname();
  const isBar = variant === 'bar';

  return (
    <nav
      aria-label="주요 메뉴"
      className={cn(isBar ? 'grid grid-cols-5 gap-1' : 'flex flex-col gap-1.5')}
    >
      {NAV.map(({ href, label, icon: Icon }) => {
        const active = isActive(pathname, href);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? 'page' : undefined}
            className={cn(
              'group relative flex items-center rounded-[var(--radius)] font-semibold',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
              isBar
                ? 'flex-col justify-center gap-1 px-2 py-2 text-[0.7rem]'
                : 'ax-float gap-3 px-4 py-3 text-[0.95rem]',
              active
                ? 'bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)] shadow-sm'
                : 'text-muted-foreground hover:bg-secondary hover:text-foreground',
            )}
          >
            <Icon aria-hidden className="h-5 w-5 shrink-0" />
            <span className={cn(isBar ? 'truncate' : 'tracking-tight')}>{label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
