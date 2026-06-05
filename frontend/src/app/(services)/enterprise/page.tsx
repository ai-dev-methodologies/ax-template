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
import { Card, CardContent } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { useAuditLogs } from '@/features/auditLog/hooks';
import { useApprovalInbox } from '@/features/approvals/hooks';
import { useFeatureFlags } from '@/features/featureFlags/hooks';
import { useSessions } from '@/features/sessions/hooks';
import { useExportJobs } from '@/features/exports/hooks';
import { PageHeader } from './_components/console-ui';

interface MetricProps {
  value: number | undefined;
  loading: boolean;
  error: boolean;
}

function Metric({ value, loading, error }: MetricProps) {
  if (loading) return <Spinner className="h-4 w-4 text-muted-foreground" label="집계 중" />;
  if (error) return <span className="text-sm font-medium text-[var(--ax-status-danger-fg)]">오류</span>;
  return <span className="text-2xl font-semibold tabular-nums tracking-tight text-foreground">{value ?? 0}</span>;
}

export default function EnterpriseOverviewPage() {
  const audit = useAuditLogs({ page: 0, size: 1 });
  const inbox = useApprovalInbox();
  const flags = useFeatureFlags();
  const sessions = useSessions();
  const exports = useExportJobs();

  const enabledFlags = flags.data?.content.filter((f) => f.enabled).length;

  const tiles = [
    {
      href: '/enterprise/audit-logs',
      label: '감사 로그',
      icon: ScrollText,
      caption: '전체 기록',
      metric: <Metric value={audit.data?.totalElements} loading={audit.isLoading} error={audit.isError} />,
    },
    {
      href: '/enterprise/approvals',
      label: '결재함',
      icon: ClipboardCheck,
      caption: '내가 처리할 건',
      metric: <Metric value={inbox.data?.totalElements} loading={inbox.isLoading} error={inbox.isError} />,
    },
    {
      href: '/enterprise/feature-flags',
      label: '기능 플래그',
      icon: ToggleRight,
      caption: flags.data ? `활성 ${enabledFlags ?? 0} / 전체 ${flags.data.totalElements}` : '전체',
      metric: <Metric value={flags.data?.totalElements} loading={flags.isLoading} error={flags.isError} />,
    },
    {
      href: '/enterprise/sessions',
      label: '세션',
      icon: MonitorSmartphone,
      caption: '내 활성 세션',
      metric: <Metric value={sessions.data?.totalElements} loading={sessions.isLoading} error={sessions.isError} />,
    },
    {
      href: '/enterprise/exports',
      label: '리포트 추출',
      icon: Download,
      caption: '추출 작업',
      metric: <Metric value={exports.data?.totalElements} loading={exports.isLoading} error={exports.isError} />,
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
        {tiles.map(({ href, label, icon: Icon, caption, metric }) => (
          <Link
            key={href}
            href={href}
            className="group rounded-[calc(var(--radius)+0.35rem)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <Card className="h-full shadow-none transition-colors duration-150 group-hover:border-foreground/20 motion-reduce:transition-none">
              <CardContent className="flex h-full flex-col gap-4 p-5">
                <div className="flex items-center justify-between">
                  <span className="grid h-9 w-9 place-items-center rounded-[var(--radius)] bg-secondary text-muted-foreground">
                    <Icon aria-hidden className="h-4 w-4" />
                  </span>
                  <ArrowRight
                    aria-hidden
                    className="h-4 w-4 text-muted-foreground transition-transform duration-150 group-hover:translate-x-0.5 motion-reduce:transform-none"
                  />
                </div>
                <div className="mt-auto space-y-0.5">
                  {metric}
                  <p className="text-sm font-medium text-foreground">{label}</p>
                  <p className="text-xs text-muted-foreground">{caption}</p>
                </div>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
