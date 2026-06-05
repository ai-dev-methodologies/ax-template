'use client';

import { Badge } from '@/components/ui/badge';

type Tone = 'success' | 'danger' | 'warning' | 'info' | 'neutral' | 'accent';

const TONES: Record<string, Tone> = {
  // approvals
  APPROVED: 'success',
  REJECTED: 'danger',
  PENDING: 'warning',
  SUBMITTED: 'info',
  DRAFT: 'neutral',
  CANCELLED: 'neutral',
  // exports
  COMPLETED: 'success',
  RUNNING: 'info',
  FAILED: 'danger',
  // sessions
  ACTIVE: 'success',
  REVOKED: 'neutral',
  // audit outcome
  SUCCESS: 'success',
  FAILURE: 'danger',
};

const LABELS: Record<string, string> = {
  APPROVED: '승인됨',
  REJECTED: '반려됨',
  PENDING: '대기',
  SUBMITTED: '제출됨',
  DRAFT: '작성 중',
  CANCELLED: '취소됨',
  COMPLETED: '완료',
  RUNNING: '처리 중',
  FAILED: '실패',
  ACTIVE: '활성',
  REVOKED: '해제됨',
  SUCCESS: '성공',
  FAILURE: '실패',
};

/** Maps a backend enum value to a token-driven, Korean-labelled status pill. */
export function StatusBadge({ status }: { status: string }) {
  return <Badge tone={TONES[status] ?? 'neutral'}>{LABELS[status] ?? status}</Badge>;
}
