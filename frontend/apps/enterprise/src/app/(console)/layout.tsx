'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { LogOut, ShieldX } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Card, CardContent, Spinner } from '@ax/ui';
import { Wordmark } from '@/components/wordmark';
import { ConsoleNav } from '@/components/console-nav';
import { useOperator } from '@/features/operator/hooks';

/**
 * Enterprise Operations Console shell.
 *
 * Persona theme (enterprise-operator) is applied on this wrapper via the
 * `.ax-operator` class (globals.css): radius 6px (--radius override), FLAT
 * elevation (shadows collapsed to a hairline ring), LOW accent saturation, and
 * state-feedback-only motion. The persona is scoped here so it never leaks past
 * the console while every catalog component inherits it through the tokens.
 *
 * Guards: redirect to /login without a token (hydrated from the shared @ax/core
 * cookie), then gate the console on the live `/api/auth/me` role === 'ADMIN'.
 */
export default function ConsoleLayout({ children }: { children: React.ReactNode }) {
  const { accessToken, hydrated, hydrate, logout } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  useEffect(() => {
    if (hydrated && !accessToken) router.replace('/login');
  }, [hydrated, accessToken, router]);

  const operator = useOperator(Boolean(accessToken));

  const handleLogout = async (): Promise<void> => {
    await logout();
    router.push('/login');
  };

  if (!hydrated || (accessToken && operator.isLoading)) {
    return (
      <div className="ax-operator grid min-h-dvh place-items-center bg-background">
        <Spinner className="h-6 w-6 text-muted-foreground" label="콘솔 불러오는 중" />
      </div>
    );
  }

  if (!accessToken) return null;

  const isAdmin = operator.data?.role === 'ADMIN';

  return (
    <div className="ax-operator flex min-h-dvh flex-col bg-background">
      <header className="sticky top-0 z-40 border-b border-border bg-background">
        <div className="mx-auto flex h-14 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-3">
            <Link
              href="/"
              aria-label="개요"
              className="rounded-[var(--radius)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
            >
              <Wordmark size="sm" />
            </Link>
            <span className="hidden font-mono text-[0.7rem] uppercase tracking-[0.16em] text-muted-foreground sm:inline">
              운영 콘솔
            </span>
          </div>
          <div className="flex items-center gap-2 sm:gap-3">
            {operator.data?.email && (
              <span className="hidden max-w-[14rem] truncate text-sm text-muted-foreground md:inline">
                {operator.data.email}
              </span>
            )}
            <Button variant="outline" size="sm" onClick={handleLogout}>
              <LogOut aria-hidden />
              <span className="hidden sm:inline">로그아웃</span>
            </Button>
          </div>
        </div>
      </header>

      {isAdmin ? (
        <div className="mx-auto flex w-full max-w-7xl flex-1 gap-6 px-4 py-6 sm:px-6 lg:gap-8">
          <aside className="hidden w-56 shrink-0 lg:block">
            <div className="sticky top-20">
              <ConsoleNav />
            </div>
          </aside>
          <main className="ax-fade-up min-w-0 flex-1">{children}</main>
        </div>
      ) : (
        <main className="mx-auto grid w-full max-w-2xl flex-1 place-items-center px-4 py-16 sm:px-6">
          <Card role="alert" className="w-full p-8 text-center">
            <CardContent className="p-0">
              <span className="mx-auto mb-4 grid h-12 w-12 place-items-center rounded-full bg-[var(--ax-status-warning-bg)] text-[var(--ax-status-warning-fg)]">
                <ShieldX aria-hidden className="h-6 w-6" />
              </span>
              <h1 className="text-xl font-semibold tracking-tight text-foreground">관리자 권한이 필요합니다</h1>
              <p className="mx-auto mt-2 max-w-sm text-sm text-muted-foreground">
                운영 콘솔은 ADMIN 역할에게만 열립니다. 현재 계정 역할은{' '}
                <span className="font-mono text-foreground">{operator.data?.role ?? '알 수 없음'}</span>
                {' '}입니다.
              </p>
              <Button variant="outline" size="sm" className="mt-6" onClick={handleLogout}>
                <LogOut aria-hidden />
                다른 계정으로 로그인
              </Button>
            </CardContent>
          </Card>
        </main>
      )}
    </div>
  );
}
