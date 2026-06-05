'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { ArrowLeft, CheckCircle2, XCircle } from 'lucide-react';
import { useAuthStore, setAccessTokenCookie } from '@ax/core';
import { Wordmark } from '@/components/brand/wordmark';
import {
  Button,
  Spinner,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@ax/ui';

type Status = 'processing' | 'success' | 'error';

const REDIRECT_DELAY_MS = 1000;

export function OAuthCallbackClient() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<Status>('processing');
  const [message, setMessage] = useState('OAuth 로그인을 처리하는 중입니다...');
  const router = useRouter();

  useEffect(() => {
    const accessToken =
      searchParams.get('accessToken') || searchParams.get('access_token');
    const error = searchParams.get('error');

    if (error) {
      setStatus('error');
      setMessage(`OAuth 로그인 실패: ${error}`);
      return;
    }

    if (accessToken) {
      // Persist to cookie (survives reload + satisfies middleware) and hydrate store.
      setAccessTokenCookie(accessToken);
      useAuthStore.setState({ accessToken });
      void useAuthStore.getState().fetchMe();
      setStatus('success');
      setMessage('로그인에 성공했습니다. 대시보드로 이동합니다...');
      const timer = setTimeout(() => router.push('/dashboard'), REDIRECT_DELAY_MS);
      return () => clearTimeout(timer);
    }

    setStatus('error');
    setMessage('OAuth 응답에 토큰이 없습니다. 서버 콜백 구현을 확인하세요.');
  }, [searchParams, router]);

  return (
    <main className="ax-auth-backdrop ax-grain flex min-h-dvh flex-col items-center justify-center px-4 py-12">
      <div className="ax-fade-up w-full max-w-[26rem]">
        <div className="mb-8 flex justify-center">
          <Wordmark size="lg" />
        </div>

        <Card className="shadow-xl shadow-foreground/5">
          <CardHeader className="items-center text-center">
            <span
              className={
                status === 'processing'
                  ? 'mb-2 grid h-14 w-14 place-items-center rounded-full bg-muted text-muted-foreground'
                  : status === 'success'
                    ? 'mb-2 grid h-14 w-14 place-items-center rounded-full bg-[var(--ax-status-success-bg)] text-[var(--ax-status-success-fg)]'
                    : 'mb-2 grid h-14 w-14 place-items-center rounded-full bg-[var(--ax-status-danger-bg)] text-[var(--ax-status-danger-fg)]'
              }
            >
              {status === 'processing' && <Spinner className="h-7 w-7" label="처리 중" />}
              {status === 'success' && <CheckCircle2 aria-hidden="true" className="h-7 w-7" />}
              {status === 'error' && <XCircle aria-hidden="true" className="h-7 w-7" />}
            </span>
            <CardTitle as="h1" className="text-2xl">OAuth 로그인</CardTitle>
            <CardDescription role="status">{message}</CardDescription>
          </CardHeader>

          {status === 'error' && (
            <CardContent>
              <Button asChild className="w-full" size="lg" variant="outline">
                <Link href="/login">
                  <ArrowLeft aria-hidden="true" />
                  로그인으로 돌아가기
                </Link>
              </Button>
            </CardContent>
          )}
        </Card>
      </div>
    </main>
  );
}
