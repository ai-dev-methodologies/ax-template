'use client';

import React, { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowRight, Sparkles } from 'lucide-react';
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
    <main className="ax-consumer flex min-h-dvh flex-col items-center justify-center bg-background px-4 py-12">
      <div className="ax-fade-up w-full max-w-[24rem]">
        <div className="mb-8 flex justify-center">
          <Wordmark size="lg" />
        </div>

        <Card className="shadow-lg">
          <CardHeader className="space-y-2">
            <span className="inline-flex h-11 w-11 items-center justify-center rounded-[var(--radius)] bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]">
              <Sparkles aria-hidden className="h-5 w-5" />
            </span>
            <CardTitle as="h1" className="text-2xl">반가워요! 👋</CardTitle>
            <CardDescription>로그인하고 친구들의 소식을 만나보세요.</CardDescription>
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
                {isLoading ? '로그인 중' : '로그인하고 시작하기'}
                {!isLoading && <ArrowRight aria-hidden />}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          ax moment · 우리들의 소셜 피드
        </p>
      </div>
    </main>
  );
}
