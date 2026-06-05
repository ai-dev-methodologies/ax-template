'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, LogOut, ShieldX } from 'lucide-react';
import { useAuthStore } from '@/lib/auth/authStore';
import { useOperator } from '@/features/operator/hooks';
import { Wordmark } from '@/components/brand/wordmark';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { ThemeToggle } from '@/components/theme-toggle';
import { ConsoleNav } from './_components/console-shell';

/**
 * Enterprise Operations Console shell.
 *
 * Persona theme (enterprise-operator): radius ~6px, low accent saturation, flat
 * elevation, state-feedback-only motion. The theme is scoped to this route group
 * via the `--radius` override + `ax-console` wrapper so it never leaks into the
 * rest of the app while still reusing every design-system token + component.
 *
 * Guards: redirect to /login without a token (client mirror of middleware), then
 * gate the console on ROLE_ADMIN from the live `/api/auth/me` projection.
 */
export default function EnterpriseLayout({ children }: { children: React.ReactNode }) {
  const { accessToken, user, hydrated, hydrate, logout } = useAuthStore();
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
      <div className="ax-console grid min-h-dvh place-items-center bg-background">
        <Spinner className="h-6 w-6 text-muted-foreground" label="콘솔 불러오는 중" />
      </div>
    );
  }

  if (!accessToken) return null;

  const isAdmin = operator.data?.role === 'ADMIN';

  return (
    <div className="ax-console flex min-h-dvh flex-col bg-background [--radius:6px]">
      <header className="sticky top-0 z-40 border-b border-border bg-background">
        <div className="mx-auto flex h-14 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-3">
            <Link
              href="/dashboard"
              aria-label="대시보드로 돌아가기"
              className="rounded-[var(--radius)] text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
            >
              <ArrowLeft aria-hidden className="h-4 w-4" />
            </Link>
            <Wordmark size="sm" compact />
            <span className="hidden font-mono text-[0.7rem] uppercase tracking-[0.16em] text-muted-foreground sm:inline">
              운영 콘솔
            </span>
          </div>
          <div className="flex items-center gap-2 sm:gap-3">
            {user?.email && (
              <span className="hidden max-w-[14rem] truncate text-sm text-muted-foreground md:inline">
                {user.email}
              </span>
            )}
            <ThemeToggle />
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
          <main className="min-w-0 flex-1">{children}</main>
        </div>
      ) : (
        <main className="mx-auto grid w-full max-w-2xl flex-1 place-items-center px-4 py-16 sm:px-6">
          <div
            role="alert"
            className="w-full rounded-[calc(var(--radius)+0.35rem)] border border-border bg-card p-8 text-center shadow-sm"
          >
            <span className="mx-auto mb-4 grid h-12 w-12 place-items-center rounded-full bg-[var(--ax-status-warning-bg)] text-[var(--ax-status-warning-fg)]">
              <ShieldX aria-hidden className="h-6 w-6" />
            </span>
            <h1 className="text-xl font-semibold tracking-tight text-foreground">관리자 권한이 필요합니다</h1>
            <p className="mx-auto mt-2 max-w-sm text-sm text-muted-foreground">
              운영 콘솔은 ADMIN 역할에게만 열립니다. 현재 계정 역할은{' '}
              <span className="font-mono text-foreground">{operator.data?.role ?? '알 수 없음'}</span>
              {' '}입니다. 접근이 필요하면 관리자에게 권한을 요청하세요.
            </p>
            <Button asChild variant="outline" size="sm" className="mt-6">
              <Link href="/dashboard">
                <ArrowLeft aria-hidden />
                대시보드로 돌아가기
              </Link>
            </Button>
          </div>
        </main>
      )}
    </div>
  );
}
