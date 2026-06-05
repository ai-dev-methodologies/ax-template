'use client';

import React, { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowRight, ShieldCheck } from 'lucide-react';
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
 * Standalone login for the pay console. Composes the catalog Field / Button /
 * Card / Alert and drives the SHARED @ax/core authStore (which also persists the
 * access-token cookie so a reload survives). On success, routes to the `from`
 * param (or the overview). The persona shell class (.ax-fintech) is applied so
 * the forced-light trust tokens + 8px radius + low-saturation navy accent +
 * subtle elevation + tabular figures reach every catalog component here.
 */
export default function LoginPage() {
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
    <main className="ax-fintech flex min-h-dvh flex-col items-center justify-center bg-background px-4 py-12">
      <div className="ax-fade w-full max-w-[26rem]">
        <div className="mb-9 flex justify-center">
          <Wordmark size="lg" />
        </div>

        <Card className="shadow-md">
          <CardHeader className="space-y-3">
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius)] bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]">
              <ShieldCheck aria-hidden className="h-5 w-5" />
            </span>
            <CardTitle as="h1" className="text-2xl font-semibold tracking-tight">
              결제·정산 콘솔
            </CardTitle>
            <CardDescription>
              결제 · 거래 원장 · 구독/요금제 · 정산 명세서를 한 곳에서 안전하게.
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
                {isLoading ? '인증 중' : '로그인'}
                {!isLoading && <ArrowRight aria-hidden />}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-7 text-center text-xs tracking-tight text-muted-foreground">
          ax pay · 안전한 결제·정산 콘솔
        </p>
      </div>
    </main>
  );
}
