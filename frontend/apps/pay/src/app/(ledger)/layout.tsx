'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { LogOut } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Spinner } from '@ax/ui';
import { AvatarGroup } from '@ax/blocks';
import { Wordmark } from '@/components/wordmark';
import { LedgerNav } from '@/components/ledger-nav';
import { useProfile } from '@/features/profile/hooks';

/**
 * Pay app shell (the ledger).
 *
 * Persona theme (fintech-trust) is applied on this wrapper via the `.ax-fintech`
 * class (globals.css): a FORCED light "bank statement" surface, radius 8px
 * (--radius override), SUBTLE elevation (catalog shadows become calm low-spread
 * statement-card shadows), LOW accent saturation (a desaturated trust-navy),
 * tabular figures everywhere money lives, a 15px grotesk base, and a
 * conservative mount fade (motion level 1). The persona is scoped here so it
 * never leaks past the app while every catalog component inherits it through the
 * tokens.
 *
 * Guard: redirect to /login without a token (hydrated from the shared @ax/core
 * cookie). Every pay API surface is authenticated(); the admin plan + billing
 * surfaces additionally require ROLE_ADMIN on the backend (the demo is ADMIN).
 */
export default function LedgerLayout({ children }: { children: React.ReactNode }) {
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
      <div className="ax-fintech grid min-h-dvh place-items-center bg-background">
        <Spinner className="h-6 w-6 text-muted-foreground" label="불러오는 중" />
      </div>
    );
  }

  if (!accessToken) return null;

  const email = profile.data?.email;
  const role = profile.data?.role;

  return (
    <div className="ax-fintech flex min-h-dvh flex-col bg-background pb-20 lg:pb-0">
      <header className="sticky top-0 z-40 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <Link
            href="/"
            aria-label="개요로 이동"
            className="rounded-[var(--radius)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <Wordmark size="md" />
          </Link>
          <div className="flex items-center gap-3">
            {email ? (
              <span className="flex items-center gap-2.5">
                <AvatarGroup members={[{ name: email }]} label="내 프로필" />
                <span className="hidden flex-col text-right sm:flex">
                  <span className="max-w-[14rem] truncate text-xs text-foreground">{email}</span>
                  {role ? (
                    <span className="text-[0.6rem] font-medium uppercase tracking-[0.1em] text-[var(--ax-status-accent-fg)]">
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
        <aside className="hidden w-48 shrink-0 lg:block">
          <div className="sticky top-20">
            <p className="mb-2 px-3 text-[0.6rem] font-semibold uppercase tracking-[0.14em] text-muted-foreground">
              메뉴
            </p>
            <LedgerNav variant="rail" />
          </div>
        </aside>
        <main className="ax-fade min-w-0 flex-1">{children}</main>
      </div>

      {/* Mobile bottom tab bar — sticky, thumb-reachable (5 entries). */}
      <nav
        aria-label="모바일 메뉴"
        className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/95 px-2 py-1.5 backdrop-blur lg:hidden"
      >
        <div className="mx-auto w-full max-w-6xl">
          <LedgerNav variant="bar" />
        </div>
      </nav>
    </div>
  );
}
