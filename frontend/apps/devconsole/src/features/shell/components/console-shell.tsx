'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { LogOut } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Spinner } from '@ax/ui';
import { AvatarGroup } from '@ax/blocks';
import { Wordmark } from '@/components/wordmark';
import { DevtoolNav } from '@/components/devtool-nav';
import { useProfile } from '@/features/profile/hooks';

/**
 * Developer console app shell.
 *
 * Persona theme (developer-tool) is applied on this wrapper via the `.ax-devtool`
 * class (globals.css): a FORCED dark surface (the wrapper re-declares the .dark
 * token set), radius 4px (--radius override), FLAT elevation (every catalog
 * shadow collapses to a hairline rule), LOW accent saturation (a desaturated
 * terminal-cyan accent), a dense 14px base, and a minimal mount fade (motion
 * level 1). The persona is scoped here so it never leaks past the app while every
 * catalog component inherits it through the tokens.
 *
 * Guard: redirect to /login without a token (hydrated from the shared @ax/core
 * cookie). Every console API surface is authenticated(); the webhook admin
 * surface additionally requires ROLE_ADMIN on the backend (the demo is ADMIN).
 */
export function ConsoleShell({ children }: { children: React.ReactNode }) {
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
      <div className="ax-devtool grid min-h-dvh place-items-center bg-background">
        <Spinner className="h-6 w-6 text-muted-foreground" label="불러오는 중" />
      </div>
    );
  }

  if (!accessToken) return null;

  const email = profile.data?.email;
  const role = profile.data?.role;

  return (
    <div className="ax-devtool flex min-h-dvh flex-col bg-background pb-20 lg:pb-0">
      <header className="sticky top-0 z-40 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <Link
            href="/keys"
            aria-label="콘솔 홈"
            className="rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <Wordmark size="md" />
          </Link>
          <div className="flex items-center gap-3">
            {email ? (
              <span className="flex items-center gap-2.5">
                <AvatarGroup members={[{ name: email }]} label="내 프로필" />
                <span className="hidden flex-col text-right sm:flex">
                  <span className="max-w-[14rem] truncate font-mono text-xs text-foreground">
                    {email}
                  </span>
                  {role ? (
                    <span className="font-mono text-[0.6rem] uppercase tracking-[0.1em] text-[var(--ax-status-accent-fg)]">
                      {role}
                    </span>
                  ) : null}
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

      <div className="mx-auto flex w-full max-w-6xl flex-1 gap-8 px-4 py-6 sm:px-6">
        <aside className="hidden w-44 shrink-0 lg:block">
          <div className="sticky top-20">
            <p className="mb-2 px-3 font-mono text-[0.6rem] uppercase tracking-[0.14em] text-muted-foreground">
              surfaces
            </p>
            <DevtoolNav variant="rail" />
          </div>
        </aside>
        <main className="ax-fade min-w-0 flex-1">{children}</main>
      </div>

      {/* Mobile bottom tab bar — sticky, thumb-reachable (6 entries, scrollable). */}
      <nav
        aria-label="모바일 메뉴"
        className="fixed inset-x-0 bottom-0 z-40 overflow-x-auto border-t border-border bg-background/95 px-2 py-1.5 backdrop-blur lg:hidden"
      >
        <div className="mx-auto w-full max-w-6xl">
          <DevtoolNav variant="bar" />
        </div>
      </nav>
    </div>
  );
}
