'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { ArrowRight, MailCheck } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Wordmark } from '@/components/brand/wordmark';
import {
  Button,
  Field,
  Alert,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@ax/ui';

const MIN_PASSWORD_LENGTH = 12;
const MAX_PASSWORD_LENGTH = 128;

/**
 * Signup feature container (frontend decomposition spec §7 — extracted from the route
 * app/(auth)/signup/page.tsx, which is now a thin re-export of this slice's barrel).
 */
export function SignupPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const { signup, isLoading, error } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < MIN_PASSWORD_LENGTH) {
      setPasswordError(`비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 합니다`);
      return;
    }
    setPasswordError(null);
    try {
      await signup(email, password);
      setDone(true);
    } catch {
      // store surfaces the error via `error`
    }
  };

  if (done) {
    return (
      <main className="ax-auth-backdrop ax-grain flex min-h-dvh flex-col items-center justify-center px-4 py-12">
        <div className="ax-fade-up w-full max-w-[26rem]">
          <div className="mb-8 flex justify-center">
            <Wordmark size="lg" />
          </div>
          <Card className="shadow-xl shadow-foreground/5">
            <CardHeader className="items-center text-center">
              <span className="mb-2 grid h-14 w-14 place-items-center rounded-full bg-[var(--ax-status-success-bg)] text-[var(--ax-status-success-fg)]">
                <MailCheck aria-hidden="true" className="h-7 w-7" />
              </span>
              <CardTitle as="h1" className="text-2xl">이메일을 확인하세요</CardTitle>
              <CardDescription>
                <span className="font-medium text-foreground">{email}</span> 으로 인증 메일을 보냈습니다.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Alert variant="info">
                개발 환경에서는 인증 링크가 서버 콘솔 로그에 출력됩니다.
              </Alert>
              <Button asChild className="w-full" size="lg">
                <Link href="/login">
                  로그인으로 이동
                  <ArrowRight aria-hidden="true" />
                </Link>
              </Button>
            </CardContent>
          </Card>
        </div>
      </main>
    );
  }

  return (
    <main className="ax-auth-backdrop ax-grain flex min-h-dvh flex-col items-center justify-center px-4 py-12">
      <div className="ax-fade-up w-full max-w-[26rem]">
        <div className="mb-8 flex justify-center">
          <Link href="/login" aria-label="ax-template 홈" className="rounded-[var(--radius)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background">
            <Wordmark size="lg" />
          </Link>
        </div>

        <Card className="shadow-xl shadow-foreground/5 backdrop-blur-sm">
          <CardHeader className="text-center">
            <CardTitle as="h1" className="text-2xl">회원가입</CardTitle>
            <CardDescription>몇 초면 계정을 만들 수 있습니다</CardDescription>
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
                autoComplete="new-password"
                placeholder="••••••••••••"
                minLength={MIN_PASSWORD_LENGTH}
                maxLength={MAX_PASSWORD_LENGTH}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (passwordError) setPasswordError(null);
                }}
                hint={`${MIN_PASSWORD_LENGTH}자 이상 입력하세요`}
                error={passwordError ?? undefined}
                required
              />
              <Button type="submit" className="w-full" size="lg" loading={isLoading}>
                {isLoading ? '가입 중' : '회원가입'}
                {!isLoading && <ArrowRight aria-hidden="true" />}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          이미 계정이 있으신가요?{' '}
          <Link
            href="/login"
            className="font-semibold text-foreground underline-offset-4 transition-colors hover:underline focus-visible:outline-none focus-visible:underline"
          >
            로그인
          </Link>
        </p>
      </div>
    </main>
  );
}
