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
 * Standalone login for the operator console. Composes the catalog Field /
 * Button / Card / Alert and drives the SHARED @ax/core authStore (which also
 * persists the access-token cookie so a reload survives). On success, routes to
 * the `from` param (or the console root).
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
    <main className="ax-operator flex min-h-dvh flex-col items-center justify-center bg-background px-4 py-12">
      <div className="ax-fade-up w-full max-w-[24rem]">
        <div className="mb-8 flex justify-center">
          <Wordmark size="lg" />
        </div>

        <Card>
          <CardHeader className="space-y-1.5">
            <span className="inline-flex h-9 w-9 items-center justify-center rounded-[var(--radius)] bg-secondary text-muted-foreground">
              <ShieldCheck aria-hidden className="h-4 w-4" />
            </span>
            <CardTitle as="h1" className="text-xl">운영 콘솔 로그인</CardTitle>
            <CardDescription>관리자 계정으로 로그인하여 콘솔에 접근하세요.</CardDescription>
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
                {isLoading ? '로그인 중' : '로그인'}
                {!isLoading && <ArrowRight aria-hidden />}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          enterprise-operator · ADMIN 전용 운영 콘솔
        </p>
      </div>
    </main>
  );
}
