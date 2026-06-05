'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { LogOut } from 'lucide-react';
import { useAuthStore } from '../../lib/auth/authStore';
import { Wordmark } from '@/components/brand/wordmark';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { ThemeToggle } from '@/components/theme-toggle';

interface AuthenticatedLayoutProps {
  children: React.ReactNode;
}

/**
 * Authenticated app shell. The primary guard is middleware.ts (server-side
 * cookie check); this is the client-side guard plus the chrome (topbar/nav).
 * On mount we hydrate the in-memory store from the persisted cookie, then gate
 * rendering on `hydrated` so we never flash a redirect before the cookie is read.
 */
export default function AuthenticatedLayout({ children }: AuthenticatedLayoutProps) {
  const { accessToken, user, hydrated, hydrate, logout } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  useEffect(() => {
    if (hydrated && !accessToken) {
      router.replace('/login');
    }
  }, [hydrated, accessToken, router]);

  const handleLogout = async (): Promise<void> => {
    await logout();
    router.push('/login');
  };

  // Wait for cookie hydration before deciding anything.
  if (!hydrated) {
    return (
      <div className="grid min-h-dvh place-items-center bg-background">
        <Spinner className="h-6 w-6 text-muted-foreground" label="불러오는 중" />
      </div>
    );
  }

  if (!accessToken) return null;

  return (
    <div className="flex min-h-dvh flex-col bg-background">
      <header className="sticky top-0 z-40 border-b border-border bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <Link
            href="/dashboard"
            aria-label="대시보드"
            className="rounded-[var(--radius)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <Wordmark size="sm" compact />
          </Link>

          <nav aria-label="사용자 메뉴" className="flex items-center gap-2 sm:gap-3">
            {user?.email && (
              <span className="hidden max-w-[14rem] truncate text-sm text-muted-foreground sm:inline">
                {user.email}
              </span>
            )}
            <ThemeToggle />
            <Button variant="outline" size="sm" onClick={handleLogout}>
              <LogOut aria-hidden="true" />
              <span className="hidden sm:inline">로그아웃</span>
            </Button>
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6 sm:py-10">
        {children}
      </main>
    </div>
  );
}
