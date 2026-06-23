'use client';

import React, { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowRight, Terminal } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import {
  Alert,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Field,
} from '@ax/ui';
import { Wordmark } from '@/components/wordmark';

/**
 * Standalone login for the developer console. Composes the catalog Field /
 * Button / Card / Alert and drives the SHARED @ax/core authStore (which also
 * persists the access-token cookie so a reload survives). On success, routes to
 * the `from` param (or the overview). The persona shell class (.ax-devtool) is
 * applied so the forced-dark tokens + 4px radius + low-saturation cyan accent +
 * flat surfaces reach every catalog component here.
 */
export function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login, isLoading, error, accessToken, hydrate, hydrated } = useAuthStore();
  const router = useRouter();
  const params = useSearchParams();
  const from = params.get('from') || '/';

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  // Already signed in (cookie hydrated) — skip the form.
  useEffect(() => {
    if (hydrated && accessToken) router.replace(from);
  }, [hydrated, accessToken, from, router]);

  const handleSubmit = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    try {
      await login(email, password);
      router.replace(from);
    } catch {
      // store surfaces the message via `error`
    }
  };

  return (
    <main className="ax-devtool flex min-h-dvh flex-col items-center justify-center bg-background px-4 py-12">
      <div className="ax-fade w-full max-w-[26rem]">
        <div className="mb-9 flex justify-center">
          <Wordmark size="lg" />
        </div>

        <Card>
          <CardHeader className="space-y-3">
            <span className="inline-flex h-10 w-10 items-center justify-center rounded border border-border bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]">
              <Terminal aria-hidden className="h-5 w-5" />
            </span>
            <CardTitle as="h1" className="text-2xl font-semibold tracking-tight">
              개발자 콘솔
            </CardTitle>
            <CardDescription>
              API 키 · 웹훅 · 멱등성 · 문제 응답 · 낙관적 잠금 · 요청 검증
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-5">
            {error && <Alert variant="error">{error}</Alert>}

            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              <Field
                id="email"
                label="이메일"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
              <Field
                id="password"
                label="비밀번호"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <Button type="submit" className="w-full" size="lg" loading={isLoading}>
                {isLoading ? '인증 중' : '콘솔 접속'}
                {!isLoading && <ArrowRight aria-hidden />}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-7 text-center font-mono text-xs tracking-tight text-muted-foreground">
          ax console · developer platform
        </p>
      </div>
    </main>
  );
}
