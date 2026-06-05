'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowRight } from 'lucide-react';
import { useAuthStore } from '../../../lib/auth/authStore';
import { Wordmark } from '@/components/brand/wordmark';
import { OAuthButtons } from '@/components/auth/oauth-buttons';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/field';
import { Alert } from '@/components/ui/alert';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login, isLoading, error } = useAuthStore();
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(email, password);
      router.push('/dashboard');
    } catch {
      // store surfaces the error via `error`
    }
  };

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
            <CardTitle as="h1" className="text-2xl">다시 오신 것을 환영합니다</CardTitle>
            <CardDescription>계정에 로그인하여 계속 진행하세요</CardDescription>
          </CardHeader>

          <CardContent className="space-y-5">
            {error && <Alert variant="error">{error}</Alert>}

            <OAuthButtons />

            <div className="relative py-1 text-center">
              <span aria-hidden="true" className="absolute inset-x-0 top-1/2 h-px -translate-y-1/2 bg-border" />
              <span className="relative bg-card px-3 text-xs font-medium uppercase tracking-wider text-muted-foreground">
                또는 이메일로 로그인
              </span>
            </div>

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
                {isLoading ? '로그인 중' : '이메일 로그인'}
                {!isLoading && <ArrowRight aria-hidden="true" />}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          계정이 없으신가요?{' '}
          <Link
            href="/signup"
            className="font-semibold text-foreground underline-offset-4 transition-colors hover:underline focus-visible:outline-none focus-visible:underline"
          >
            회원가입
          </Link>
        </p>
      </div>
    </main>
  );
}
