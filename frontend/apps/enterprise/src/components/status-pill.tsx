'use client';

import { Badge } from '@ax/ui';

/**
 * Maps a backend enum value (approval / export / session / audit status) to a
 * token-driven, Korean-labelled pill rendered by the SHARED catalog `Badge`.
 *
 * This is a thin domain mapping wrapper, NOT a UI primitive — it composes
 * `@ax/ui` `Badge` rather than re-implementing one. The catalog `@ax/blocks`
 * StatusBadge fixes a 7-value `StatusKind` union that cannot represent the full
 * backend status surface here (FAILURE / RUNNING / COMPLETED / CANCELLED /
 * ACTIVE / REVOKED / DRAFT …), so we drive the catalog Badge's `tone` directly.
 */

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

export function StatusPill({ status }: { status: string }) {
  return <Badge tone={TONES[status] ?? 'neutral'}>{LABELS[status] ?? status}</Badge>;
}
