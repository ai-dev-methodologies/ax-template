'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { ArrowRight, CheckCircle2, XCircle } from 'lucide-react';
import { authClient } from '../../../lib/api/authClient';
import { Wordmark } from '@/components/brand/wordmark';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

type Status = 'pending' | 'success' | 'error';

export function VerifyPageClient() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<Status>('pending');
  const [message, setMessage] = useState('이메일을 인증하는 중입니다...');

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setStatus('error');
      setMessage('인증 토큰이 없습니다.');
      return;
    }
    authClient
      .verifyEmail({ token })
      .then((res) => {
        setStatus('success');
        setMessage(res.message);
      })
      .catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : '이메일 인증에 실패했습니다.';
        setStatus('error');
        setMessage(msg);
      });
  }, [searchParams]);

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
                status === 'pending'
                  ? 'mb-2 grid h-14 w-14 place-items-center rounded-full bg-muted text-muted-foreground'
                  : status === 'success'
                    ? 'mb-2 grid h-14 w-14 place-items-center rounded-full bg-[var(--ax-status-success-bg)] text-[var(--ax-status-success-fg)]'
                    : 'mb-2 grid h-14 w-14 place-items-center rounded-full bg-[var(--ax-status-danger-bg)] text-[var(--ax-status-danger-fg)]'
              }
            >
              {status === 'pending' && <Spinner className="h-7 w-7" label="인증 중" />}
              {status === 'success' && <CheckCircle2 aria-hidden="true" className="h-7 w-7" />}
              {status === 'error' && <XCircle aria-hidden="true" className="h-7 w-7" />}
            </span>
            <CardTitle as="h1" className="text-2xl">이메일 인증</CardTitle>
            <CardDescription role="status">{message}</CardDescription>
          </CardHeader>

          {status !== 'pending' && (
            <CardContent>
              <Button asChild className="w-full" size="lg" variant={status === 'success' ? 'default' : 'outline'}>
                <Link href="/login">
                  로그인으로 이동
                  <ArrowRight aria-hidden="true" />
                </Link>
              </Button>
            </CardContent>
          )}
        </Card>
      </div>
    </main>
  );
}
