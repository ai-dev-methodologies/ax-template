'use client';

import React, { useState } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { Check, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { useApprovalRequest, useStepAction } from '@/features/approvals/hooks';
import { ErrorPanel } from '../_components/console-ui';
import { StatusBadge } from '../_components/status-badge';
import { formatTimestamp, shortenId } from '../_components/format';

interface ApprovalDrawerProps {
  requestId: string | null;
  stepId: string | null;
  onClose: () => void;
}

/**
 * Slide-in detail drawer for one approval request. Renders the full step trail
 * and offers Approve / Reject on the caller's pending step. Mutations invalidate
 * the inbox so the list reflects the new state on close.
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
    <Dialog.Root open={open} onOpenChange={(next) => !next && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-foreground/30 backdrop-blur-[1px] data-[state=open]:animate-in data-[state=open]:fade-in motion-reduce:animate-none" />
        <Dialog.Content className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-border bg-card shadow-lg focus:outline-none data-[state=open]:animate-in data-[state=open]:slide-in-from-right motion-reduce:animate-none">
          <div className="flex items-start justify-between gap-3 border-b border-border p-5">
            <div className="min-w-0 space-y-1">
              <Dialog.Title className="truncate text-base font-semibold text-foreground">
                {request?.title || (detail.isLoading ? '불러오는 중…' : '결재 상세')}
              </Dialog.Title>
              <Dialog.Description className="font-mono text-xs text-muted-foreground">
                {request ? `${request.type} · ${shortenId(request.id)}` : '결재 요청 상세 정보'}
              </Dialog.Description>
            </div>
            <Dialog.Close asChild>
              <Button variant="ghost" size="icon" aria-label="닫기">
                <X aria-hidden />
              </Button>
            </Dialog.Close>
          </div>

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
                    <dd><StatusBadge status={request.status} /></dd>
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
                        <StatusBadge status={step.status} />
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
              <Button
                variant="destructive"
                className="flex-1"
                onClick={() => act('reject')}
                loading={reject.isPending}
                disabled={busy}
              >
                <X aria-hidden />
                반려
              </Button>
              <Button
                className="flex-1"
                onClick={() => act('approve')}
                loading={approve.isPending}
                disabled={busy}
              >
                <Check aria-hidden />
                승인
              </Button>
            </div>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
