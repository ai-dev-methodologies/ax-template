'use client';

import React, { useState } from 'react';
import { Check, X } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  Spinner,
} from '@ax/ui';
import { PrimeButton } from '@ax/blocks';
import { ErrorPanel } from '@/components/console-ui';
import { StatusPill } from '@/components/status-pill';
import { formatTimestamp, shortenId } from '@/lib/format';
import { useApprovalRequest, useStepAction } from '@/features/approvals/hooks';

interface ApprovalDrawerProps {
  requestId: string | null;
  stepId: string | null;
  onClose: () => void;
}

type PrimeState = 'idle' | 'loading' | 'success' | 'error';

function primeState(isPending: boolean, isError: boolean): PrimeState {
  if (isPending) return 'loading';
  if (isError) return 'error';
  return 'idle';
}

/**
 * Slide-in detail drawer for one approval request, built on the SHARED catalog
 * Dialog (side="right"). Renders the full step trail and offers Approve / Reject
 * on the caller's pending step via the catalog @ax/blocks PrimeButton (its
 * loading/success/error states are exactly the restrained state-feedback motion
 * the persona allows). Mutations invalidate the inbox so the list reflects the
 * new state on close.
 */
export function ApprovalDrawer({ requestId, stepId, onClose }: ApprovalDrawerProps) {
  const open = Boolean(requestId);
  const detail = useApprovalRequest(requestId);
  const approve = useStepAction('approve');
  const reject = useStepAction('reject');
  const [comment, setComment] = useState('');

  const busy = approve.isPending || reject.isPending;
  const actionError = approve.error ?? reject.error;
  const request = detail.data;
  const callerStep = request?.steps.find((s) => s.id === stepId);
  const canAct = callerStep?.status === 'PENDING' && request?.status === 'SUBMITTED';

  const act = (kind: 'approve' | 'reject'): void => {
    if (!requestId || !stepId) return;
    const input = { requestId, stepId, comment: comment.trim() || undefined };
    const mutation = kind === 'approve' ? approve : reject;
    mutation.mutate(input, { onSuccess: () => setComment('') });
  };

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent side="right" className="gap-0 p-0">
        <DialogHeader className="border-b border-border p-5 pr-12">
          <DialogTitle>
            {request?.title || (detail.isLoading ? '불러오는 중…' : '결재 상세')}
          </DialogTitle>
          <DialogDescription className="font-mono text-xs">
            {request ? `${request.type} · ${shortenId(request.id)}` : '결재 요청 상세 정보'}
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 space-y-5 overflow-y-auto p-5">
          {detail.isLoading ? (
            <div className="flex items-center gap-2 py-8 text-sm text-muted-foreground">
              <Spinner className="h-4 w-4" label="상세 불러오는 중" /> 상세를 불러오는 중…
            </div>
          ) : detail.isError ? (
            <ErrorPanel message={detail.error.message} onRetry={() => detail.refetch()} />
          ) : request ? (
            <>
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <div className="space-y-0.5">
                  <dt className="text-xs uppercase tracking-wide text-muted-foreground">상태</dt>
                  <dd><StatusPill status={request.status} /></dd>
                </div>
                <div className="space-y-0.5">
                  <dt className="text-xs uppercase tracking-wide text-muted-foreground">요청자</dt>
                  <dd className="font-mono text-xs text-foreground">{shortenId(request.requesterUserId)}</dd>
                </div>
                <div className="space-y-0.5">
                  <dt className="text-xs uppercase tracking-wide text-muted-foreground">생성</dt>
                  <dd className="tabular-nums text-foreground">{formatTimestamp(request.createdAt)}</dd>
                </div>
                <div className="space-y-0.5">
                  <dt className="text-xs uppercase tracking-wide text-muted-foreground">제출</dt>
                  <dd className="tabular-nums text-foreground">{formatTimestamp(request.submittedAt)}</dd>
                </div>
              </dl>

              {Object.keys(request.payload).length > 0 && (
                <section className="space-y-1.5">
                  <h3 className="text-xs uppercase tracking-wide text-muted-foreground">요청 내용</h3>
                  <pre className="overflow-x-auto rounded-[var(--radius)] border border-border bg-secondary/50 p-3 text-xs text-foreground">
                    {JSON.stringify(request.payload, null, 2)}
                  </pre>
                </section>
              )}

              <section className="space-y-2">
                <h3 className="text-xs uppercase tracking-wide text-muted-foreground">결재선</h3>
                <ol className="space-y-2">
                  {request.steps.map((step) => (
                    <li
                      key={step.id}
                      className="flex items-center justify-between gap-2 rounded-[var(--radius)] border border-border px-3 py-2 text-sm"
                    >
                      <span className="flex items-center gap-2">
                        <span className="grid h-5 w-5 place-items-center rounded-full bg-secondary text-xs tabular-nums text-muted-foreground">
                          {step.orderIndex + 1}
                        </span>
                        <span className="font-mono text-xs text-foreground">{shortenId(step.approverUserId)}</span>
                      </span>
                      <StatusPill status={step.status} />
                    </li>
                  ))}
                </ol>
              </section>

              {canAct && (
                <section className="space-y-2 border-t border-border pt-4">
                  <label htmlFor="approval-comment" className="text-sm font-medium text-foreground">
                    의견 (선택)
                  </label>
                  <textarea
                    id="approval-comment"
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    rows={2}
                    maxLength={1024}
                    placeholder="결재 의견을 남기세요"
                    className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm text-foreground shadow-sm focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
                  />
                  {actionError ? <ErrorPanel message={actionError.message} /> : null}
                </section>
              )}
            </>
          ) : null}
        </div>

        {canAct && (
          <div className="flex items-center gap-2 border-t border-border p-5">
            <PrimeButton
              variant="destructive"
              className="flex-1"
              actionState={primeState(reject.isPending, reject.isError)}
              onClick={() => act('reject')}
              disabled={busy}
              loadingText="처리 중"
              errorText="실패"
            >
              <X aria-hidden className="h-4 w-4" />
              반려
            </PrimeButton>
            <PrimeButton
              className="flex-1"
              actionState={primeState(approve.isPending, approve.isError)}
              onClick={() => act('approve')}
              disabled={busy}
              loadingText="처리 중"
              errorText="실패"
            >
              <Check aria-hidden className="h-4 w-4" />
              승인
            </PrimeButton>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
