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

/**
 * Standalone login for the creative studio. Composes the catalog Field / Button
 * / Card / Alert and drives the SHARED @ax/core authStore (which also persists
 * the access-token cookie so a reload survives). On success, routes to the `from`
 * param (or the studio home). The persona shell class (.ax-studio) is applied so
 * the vibrant tokens + 24px radius + high-saturation accent + layered colorful
 * elevation + chunky display type + the cinematic rise reach every catalog
 * component here.
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
    <main className="ax-studio flex min-h-dvh flex-col items-center justify-center bg-background px-4 py-12">
      <div className="ax-rise w-full max-w-[27rem]">
        <div className="mb-10 flex justify-center">
          <Wordmark size="lg" />
        </div>

        <Card className="shadow-lg">
          <CardHeader className="space-y-3">
            <span className="inline-flex h-12 w-12 items-center justify-center rounded-[var(--radius)] bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]">
              <Sparkles aria-hidden className="h-6 w-6" />
            </span>
            <CardTitle as="h1" className="ax-display text-3xl font-extrabold tracking-tight">
              크리에이티브 스튜디오
            </CardTitle>
            <CardDescription className="text-base">
              작품을 올리고, 컬렉션으로 묶고, 좋아요로 반응하세요.
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
                {isLoading ? '입장 중' : '스튜디오 입장'}
                {!isLoading && <ArrowRight aria-hidden />}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-7 text-center text-xs tracking-tight text-muted-foreground">
          ax studio · 크리에이터를 위한 미디어 스튜디오
        </p>
      </div>
    </main>
  );
}
