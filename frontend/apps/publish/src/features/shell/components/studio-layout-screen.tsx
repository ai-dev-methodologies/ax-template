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
 * Editorial studio app shell.
 *
 * Persona theme (editorial-luxury) is applied on this wrapper via the
 * `.ax-editorial` class (globals.css): radius 0px (--radius override), FLAT
 * elevation (every catalog shadow collapses to a hairline rule), LOW accent
 * saturation (a near-monochrome ink accent), and a refined reveal motion budget.
 * The persona is scoped here so it never leaks past the app while every catalog
 * component inherits it through the tokens.
 *
 * Guard: redirect to /login without a token (hydrated from the shared @ax/core
 * cookie). The CMS endpoints are authenticated(); tag-definition writes require
 * ROLE_ADMIN on the backend (the demo account is ADMIN).
 */
export function StudioLayoutScreen({ children }: { children: React.ReactNode }) {
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
      <div className="ax-editorial grid min-h-dvh place-items-center bg-background">
        <Spinner className="h-6 w-6 text-muted-foreground" label="불러오는 중" />
      </div>
    );
  }

  if (!accessToken) return null;

  const email = profile.data?.email;
  const role = profile.data?.role;

  return (
    <div className="ax-editorial flex min-h-dvh flex-col bg-background pb-20 lg:pb-0">
      <header className="sticky top-0 z-40 border-b border-foreground bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between gap-4 px-4 sm:px-8">
          <Link
            href="/"
            aria-label="라이브러리 홈"
            className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <Wordmark size="md" />
          </Link>
          <div className="flex items-center gap-4">
            {email ? (
              <span className="flex items-center gap-2.5">
                <AvatarGroup members={[{ name: email }]} label="내 프로필" />
                <span className="hidden flex-col text-right sm:flex">
                  <span className="max-w-[14rem] truncate text-sm text-foreground">{email}</span>
                  {role ? (
                    <span className="text-[0.65rem] uppercase tracking-[0.12em] text-muted-foreground">
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

      <div className="mx-auto flex w-full max-w-6xl flex-1 gap-10 px-4 py-8 sm:px-8">
        <aside className="hidden w-48 shrink-0 lg:block">
          <div className="sticky top-24 border-l border-border">
            <StudioNav variant="rail" />
          </div>
        </aside>
        <main className="ax-reveal min-w-0 flex-1">{children}</main>
      </div>

      {/* Mobile bottom tab bar — sticky, thumb-reachable. */}
      <nav
        aria-label="모바일 메뉴"
        className="fixed inset-x-0 bottom-0 z-40 border-t border-foreground bg-background/95 px-2 py-1.5 backdrop-blur lg:hidden"
      >
        <div className="mx-auto w-full max-w-6xl">
          <StudioNav variant="bar" />
        </div>
      </nav>
    </div>
  );
}
