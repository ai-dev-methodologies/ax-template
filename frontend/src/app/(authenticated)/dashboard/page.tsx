'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  ArrowRight,
  CheckCircle2,
  Clock,
  Link2,
  Mail,
  RefreshCw,
  ShieldCheck,
  SlidersHorizontal,
  UserRound,
} from 'lucide-react';
import { useAuthStore } from '../../../lib/auth/authStore';
import { Alert } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

const PROVIDER_LABEL: Record<string, string> = {
  google: 'Google',
  kakao: 'Kakao',
  naver: 'Naver',
};

export default function DashboardPage() {
  const { user, accessToken, fetchMe, meError } = useAuthStore();
  const [retrying, setRetrying] = useState(false);

  useEffect(() => {
    if (accessToken && !user) fetchMe();
  }, [accessToken, user, fetchMe]);

  const handleRetry = async (): Promise<void> => {
    setRetrying(true);
    try {
      await fetchMe();
    } finally {
      setRetrying(false);
    }
  };

  const isVerified = user?.verificationState === 'verified';
  // Profile load failed and we have nothing to show — surface a retryable error
  // instead of an indefinite spinner. (401/403 already cleared the session.)
  const showLoadError = !user && Boolean(meError);

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <p className="font-mono text-xs uppercase tracking-[0.18em] text-muted-foreground">
          Dashboard
        </p>
        <h1 className="text-3xl font-semibold tracking-tight text-foreground">개요</h1>
        <p className="text-sm text-muted-foreground">계정 상태와 프로필 정보를 확인하세요.</p>
      </header>

      {showLoadError ? (
        <Alert variant="error">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <span className="leading-relaxed">
              프로필을 불러오지 못했습니다. {meError}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={handleRetry}
              loading={retrying}
              className="shrink-0"
            >
              <RefreshCw aria-hidden="true" />
              다시 시도
            </Button>
          </div>
        </Alert>
      ) : !user ? (
        <Card>
          <CardContent className="flex items-center gap-3 py-10 text-sm text-muted-foreground">
            <Spinner className="h-5 w-5" label="프로필 로딩 중" />
            프로필을 불러오는 중입니다...
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-5 md:grid-cols-3">
          {/* Identity — spans wider for hierarchy */}
          <Card className="md:col-span-2">
            <CardHeader className="flex-row items-start gap-4 space-y-0">
              <span className="grid h-12 w-12 shrink-0 place-items-center rounded-full bg-secondary text-secondary-foreground">
                <UserRound aria-hidden="true" className="h-6 w-6" />
              </span>
              <div className="min-w-0 flex-1">
                <CardTitle className="truncate text-lg">{user.email}</CardTitle>
                <CardDescription className="font-mono text-xs">
                  ID · {user.userId}
                </CardDescription>
              </div>
              <Badge tone={isVerified ? 'success' : 'warning'}>
                {isVerified ? (
                  <CheckCircle2 aria-hidden="true" className="h-3 w-3" />
                ) : (
                  <Clock aria-hidden="true" className="h-3 w-3" />
                )}
                {isVerified ? '인증됨' : '인증 대기'}
              </Badge>
            </CardHeader>
            <CardContent>
              <dl className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-1">
                  <dt className="flex items-center gap-1.5 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    <Mail aria-hidden="true" className="h-3.5 w-3.5" /> 이메일 인증
                  </dt>
                  <dd className="text-sm text-foreground">
                    {isVerified ? '완료됨' : '미완료 — 받은 편지함을 확인하세요'}
                  </dd>
                </div>
                <div className="space-y-1">
                  <dt className="flex items-center gap-1.5 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    <ShieldCheck aria-hidden="true" className="h-3.5 w-3.5" /> 역할
                  </dt>
                  <dd className="flex flex-wrap gap-1.5">
                    {user.roles?.length ? (
                      user.roles.map((role) => (
                        <Badge key={role} tone="info">
                          {role}
                        </Badge>
                      ))
                    ) : (
                      <span className="text-sm text-muted-foreground">없음</span>
                    )}
                  </dd>
                </div>
              </dl>
            </CardContent>
          </Card>

          {/* Verification status callout */}
          <Card
            className={
              isVerified
                ? 'border-[color-mix(in_oklab,var(--ax-status-success-fg)_25%,transparent)] bg-[var(--ax-status-success-bg)]'
                : 'border-[color-mix(in_oklab,var(--ax-status-warning-fg)_25%,transparent)] bg-[var(--ax-status-warning-bg)]'
            }
          >
            <CardHeader>
              <span
                className={
                  isVerified
                    ? 'mb-1 grid h-10 w-10 place-items-center rounded-full bg-card text-[var(--ax-status-success-fg)]'
                    : 'mb-1 grid h-10 w-10 place-items-center rounded-full bg-card text-[var(--ax-status-warning-fg)]'
                }
              >
                <ShieldCheck aria-hidden="true" className="h-5 w-5" />
              </span>
              <CardTitle
                className={
                  isVerified
                    ? 'text-base text-[var(--ax-status-success-fg)]'
                    : 'text-base text-[var(--ax-status-warning-fg)]'
                }
              >
                {isVerified ? '계정 보호 활성' : '계정 인증 필요'}
              </CardTitle>
              <CardDescription
                className={
                  isVerified
                    ? 'text-[color-mix(in_oklab,var(--ax-status-success-fg)_80%,transparent)]'
                    : 'text-[color-mix(in_oklab,var(--ax-status-warning-fg)_80%,transparent)]'
                }
              >
                {isVerified
                  ? '모든 기능을 사용할 수 있습니다.'
                  : '이메일 인증을 완료하면 모든 기능이 열립니다.'}
              </CardDescription>
            </CardHeader>
          </Card>

          {/* Linked providers */}
          <Card className="md:col-span-3">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Link2 aria-hidden="true" className="h-4 w-4 text-muted-foreground" />
                연결된 소셜 계정
              </CardTitle>
              <CardDescription>로그인에 사용할 수 있는 외부 제공자입니다.</CardDescription>
            </CardHeader>
            <CardContent>
              {user.providerLinks?.length ? (
                <ul className="flex flex-wrap gap-2">
                  {user.providerLinks.map((link) => (
                    <li key={`${link.provider}-${link.connectedAt}`}>
                      <Badge tone="accent" className="px-3 py-1 text-sm">
                        {PROVIDER_LABEL[link.provider] ?? link.provider}
                      </Badge>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground">
                  아직 연결된 소셜 계정이 없습니다.
                </p>
              )}
            </CardContent>
          </Card>

          {/* Service launcher — enterprise operations console (ADMIN) */}
          <Link
            href="/enterprise"
            className="group md:col-span-3 rounded-[calc(var(--radius)+0.35rem)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <Card className="transition-colors duration-150 group-hover:border-foreground/20 motion-reduce:transition-none">
              <CardHeader className="flex-row items-center gap-4 space-y-0">
                <span className="grid h-11 w-11 shrink-0 place-items-center rounded-[var(--radius)] bg-secondary text-secondary-foreground">
                  <SlidersHorizontal aria-hidden="true" className="h-5 w-5" />
                </span>
                <div className="min-w-0 flex-1">
                  <CardTitle className="flex items-center gap-2 text-base">
                    운영 콘솔
                    <Badge tone="info">ADMIN</Badge>
                  </CardTitle>
                  <CardDescription>
                    감사 로그 · 결재함 · 기능 플래그 · 세션 · 리포트 추출을 한 곳에서 운영합니다.
                  </CardDescription>
                </div>
                <ArrowRight
                  aria-hidden="true"
                  className="h-5 w-5 shrink-0 text-muted-foreground transition-transform duration-150 group-hover:translate-x-0.5 motion-reduce:transform-none"
                />
              </CardHeader>
            </Card>
          </Link>
        </div>
      )}
    </div>
  );
}
