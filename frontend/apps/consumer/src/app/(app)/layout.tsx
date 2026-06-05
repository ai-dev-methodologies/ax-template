'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { LogOut } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Spinner } from '@ax/ui';
import { AvatarGroup } from '@ax/blocks';
import { Wordmark } from '@/components/wordmark';
import { ConsumerNav } from '@/components/consumer-nav';
import { useProfile } from '@/features/profile/hooks';
import { useUnreadCount } from '@/features/notifications/hooks';

/**
 * Consumer feed app shell.
 *
 * Persona theme (consumer-delight) is applied on this wrapper via the
 * `.ax-consumer` class (globals.css): radius 20px (--radius override), LAYERED
 * elevation (soft accent-tinted shadows), HIGH accent saturation, and an
 * entrance + hover-spring motion budget. The persona is scoped here so it never
 * leaks past the app while every catalog component inherits it through the tokens.
 *
 * Guard: redirect to /login without a token (hydrated from the shared @ax/core
 * cookie). Unlike the operator console there is NO role gate — every
 * authenticated user may use the feed.
 */
export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { accessToken, hydrated, hydrate, logout } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  useEffect(() => {
    if (hydrated && !accessToken) router.replace('/login');
  }, [hydrated, accessToken, router]);

  const enabled = Boolean(accessToken);
  const profile = useProfile(enabled);
  const unread = useUnreadCount(enabled);

  const handleLogout = async (): Promise<void> => {
    await logout();
    router.push('/login');
  };

  if (!hydrated) {
    return (
      <div className="ax-consumer grid min-h-dvh place-items-center bg-background">
        <Spinner className="h-6 w-6 text-muted-foreground" label="불러오는 중" />
      </div>
    );
  }

  if (!accessToken) return null;

  const email = profile.data?.email;
  const unreadCount = unread.data ?? 0;

  return (
    <div className="ax-consumer flex min-h-dvh flex-col bg-background pb-20 lg:pb-0">
      <header className="sticky top-0 z-40 border-b border-border bg-background/85 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-5xl items-center justify-between gap-4 px-4 sm:px-6">
          <Link
            href="/"
            aria-label="피드 홈"
            className="rounded-[var(--radius)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <Wordmark size="md" />
          </Link>
          <div className="flex items-center gap-3">
            {email ? (
              <span className="flex items-center gap-2">
                <AvatarGroup members={[{ name: email }]} label="내 프로필" />
                <span className="hidden max-w-[12rem] truncate text-sm text-muted-foreground sm:inline">
                  {email}
                </span>
              </span>
            ) : null}
            <Button variant="ghost" size="sm" onClick={handleLogout}>
              <LogOut aria-hidden />
              <span className="sr-only sm:not-sr-only">로그아웃</span>
            </Button>
          </div>
        </div>
      </header>

      <div className="mx-auto flex w-full max-w-5xl flex-1 gap-8 px-4 py-6 sm:px-6">
        <aside className="hidden w-52 shrink-0 lg:block">
          <div className="sticky top-24">
            <ConsumerNav unreadCount={unreadCount} variant="rail" />
          </div>
        </aside>
        <main className="ax-fade-up min-w-0 flex-1">{children}</main>
      </div>

      {/* Mobile bottom tab bar — sticky, thumb-reachable. */}
      <nav
        aria-label="모바일 메뉴"
        className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/95 px-2 py-1.5 backdrop-blur lg:hidden"
      >
        <div className="mx-auto w-full max-w-5xl">
          <ConsumerNav unreadCount={unreadCount} variant="bar" />
        </div>
      </nav>
    </div>
  );
}
