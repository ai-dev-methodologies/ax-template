'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { LogOut } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Spinner } from '@ax/ui';
import { AvatarGroup } from '@ax/blocks';
import { Wordmark } from '@/components/wordmark';
import { StudioNav } from '@/components/studio-nav';
import { useProfile } from '@/features/profile/hooks';

/**
 * Creative-studio app shell.
 *
 * Persona theme (playful-creator) is applied on this wrapper via the `.ax-studio`
 * class (globals.css): radius 24px (--radius override), LAYERED colorful
 * elevation, HIGH accent saturation, chunky rounded display type, and the
 * richest motion budget of the six personas (cinematic rise + reactions). The
 * persona is scoped here so it never leaks past the app while every catalog
 * component inherits it through the tokens. The motion is fully calmed under
 * prefers-reduced-motion by the reduce block in globals.css.
 *
 * Guard: redirect to /login without a token (hydrated from the shared @ax/core
 * cookie). Every authenticated user may use the studio; files are owner-scoped
 * by the JWT on the backend.
 */
export function StudioShell({ children }: { children: React.ReactNode }) {
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

  const handleLogout = async (): Promise<void> => {
    await logout();
    router.push('/login');
  };

  if (!hydrated) {
    return (
      <div className="ax-studio grid min-h-dvh place-items-center bg-background">
        <Spinner className="h-7 w-7 text-[var(--ax-status-accent-fg)]" label="불러오는 중" />
      </div>
    );
  }

  if (!accessToken) return null;

  const email = profile.data?.email;

  return (
    <div className="ax-studio flex min-h-dvh flex-col bg-background pb-20 lg:pb-0">
      <header className="sticky top-0 z-40 border-b border-border bg-background/80 backdrop-blur-md">
        <div className="mx-auto flex h-[4.5rem] w-full max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <Link
            href="/"
            aria-label="스튜디오 홈"
            className="rounded-[var(--radius)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <Wordmark size="md" />
          </Link>
          <div className="flex items-center gap-3">
            {email ? (
              <span className="flex items-center gap-2">
                <AvatarGroup members={[{ name: email }]} label="내 프로필" />
                <span className="hidden max-w-[12rem] truncate text-sm font-medium text-muted-foreground sm:inline">
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

      <div className="mx-auto flex w-full max-w-6xl flex-1 gap-8 px-4 py-7 sm:px-6">
        <aside className="hidden w-56 shrink-0 lg:block">
          <div className="sticky top-28">
            <StudioNav variant="rail" />
          </div>
        </aside>
        <main className="ax-rise min-w-0 flex-1">{children}</main>
      </div>

      {/* Mobile bottom tab bar — sticky, thumb-reachable. */}
      <nav
        aria-label="모바일 메뉴"
        className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/95 px-2 py-1.5 backdrop-blur lg:hidden"
      >
        <div className="mx-auto w-full max-w-6xl">
          <StudioNav variant="bar" />
        </div>
      </nav>
    </div>
  );
}
