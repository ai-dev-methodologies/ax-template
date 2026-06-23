'use client';

import Link from 'next/link';
import {
  ArrowRight,
  ClipboardCheck,
  Download,
  MonitorSmartphone,
  ScrollText,
  ToggleRight,
} from 'lucide-react';
import {
  InterfacesCard,
  InterfacesCardContent,
  InterfacesCardHeader,
  InterfacesCardTitle,
  CategoryBarChart,
} from '@ax/blocks';
import { Spinner } from '@ax/ui';
import { PageHeader } from '@/components/console-ui';
import { useAuditLogs } from '@/features/auditLog/hooks';
import { useApprovalInbox } from '@/features/approvals/hooks';
import { useFeatureFlags } from '@/features/featureFlags/hooks';
import { useSessions } from '@/features/sessions/hooks';
import { useExportJobs } from '@/features/exports/hooks';

interface MetricProps {
  value: number | undefined;
  loading: boolean;
  error: boolean;
}

function Metric({ value, loading, error }: MetricProps) {
  if (loading) return <Spinner className="h-4 w-4 text-muted-foreground" label="집계 중" />;
  if (error) return <span className="text-sm font-medium text-[var(--ax-status-danger-fg)]">오류</span>;
  return <span className="text-3xl font-semibold tabular-nums tracking-tight text-foreground">{value ?? 0}</span>;
}

export function OverviewScreen() {
  const audit = useAuditLogs({ page: 0, size: 1 });
  const inbox = useApprovalInbox();
  const flags = useFeatureFlags();
  const sessions = useSessions();
  const exports = useExportJobs();

  const enabledFlags = flags.data?.content.filter((f) => f.enabled).length;

  const tiles = [
    {
      href: '/audit-logs',
      label: '감사 로그',
      icon: ScrollText,
      caption: '전체 기록',
      query: audit,
      value: audit.data?.totalElements,
    },
    {
      href: '/approvals',
      label: '결재함',
      icon: ClipboardCheck,
      caption: '내가 처리할 건',
      query: inbox,
      value: inbox.data?.totalElements,
    },
    {
      href: '/feature-flags',
      label: '기능 플래그',
      icon: ToggleRight,
      caption: flags.data ? `활성 ${enabledFlags ?? 0} / 전체 ${flags.data.totalElements}` : '전체',
      query: flags,
      value: flags.data?.totalElements,
    },
    {
      href: '/sessions',
      label: '세션',
      icon: MonitorSmartphone,
      caption: '내 활성 세션',
      query: sessions,
      value: sessions.data?.totalElements,
    },
    {
      href: '/exports',
      label: '리포트 추출',
      icon: Download,
      caption: '추출 작업',
      query: exports,
      value: exports.data?.totalElements,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Operations Console"
        title="운영 개요"
        description="감사 · 결재 · 기능 플래그 · 세션 · 리포트를 한 곳에서 운영합니다."
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {tiles.map(({ href, label, icon: Icon, caption, query, value }) => (
          <Link
            key={href}
            href={href}
            className="group rounded-[calc(var(--radius)+0.35rem)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <InterfacesCard className="h-full transition-colors duration-150 group-hover:border-foreground/25 motion-reduce:transition-none">
              <InterfacesCardHeader className="border-b-0 pb-0">
                <div className="flex items-center justify-between">
                  <span className="grid h-9 w-9 place-items-center rounded-[var(--radius)] bg-secondary text-muted-foreground">
                    <Icon aria-hidden className="h-4 w-4" />
                  </span>
                  <ArrowRight
                    aria-hidden
                    className="h-4 w-4 text-muted-foreground transition-transform duration-150 group-hover:translate-x-0.5 motion-reduce:transform-none"
                  />
                </div>
              </InterfacesCardHeader>
              <InterfacesCardContent className="space-y-0.5 pt-3">
                <Metric value={value} loading={query.isLoading} error={query.isError} />
                <InterfacesCardTitle className="text-sm font-medium text-foreground">
                  {label}
                </InterfacesCardTitle>
                <p className="text-xs text-muted-foreground">{caption}</p>
              </InterfacesCardContent>
            </InterfacesCard>
          </Link>
        ))}
      </div>

      <section aria-labelledby="channels-heading" className="space-y-3">
        <h2 id="channels-heading" className="text-sm font-medium text-muted-foreground">
          한눈에 보기
        </h2>
        <CategoryBarChart />
      </section>
    </div>
  );
}
