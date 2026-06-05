'use client';

import React, { useMemo, useState } from 'react';
import { CornerDownRight, Pencil, Send, Trash2 } from 'lucide-react';
import { Button, Card, CardContent, ConfirmDialog, Input } from '@ax/ui';
import { AvatarGroup } from '@ax/blocks';
import { DELETED_BODY_MASK, type Comment } from '@/lib/api/commentClient';
import {
  useComments,
  useCreateComment,
  useDeleteComment,
  useEditComment,
} from '@/features/comments/hooks';
import { displayName, formatRelative } from '@/lib/format';
import { errorMessage } from '@/lib/errors';
import { ErrorState, LoadingState } from '@/components/screen-states';

interface CommentThreadProps {
  entityType: string;
  entityId: string;
  /** Caller's user id — gates the edit/delete affordances to own comments. */
  currentUserId?: string;
  enabled: boolean;
}

interface CommentNode extends Comment {
  replies: Comment[];
}

/** Group a flat comment list into roots + their direct replies (one level). */
function buildTree(items: Comment[]): CommentNode[] {
  const roots = items.filter((c) => c.parentCommentId === null);
  return roots.map((root) => ({
    ...root,
    replies: items.filter((c) => c.parentCommentId === root.id),
  }));
}

/**
 * App-local comment thread — a COMPOSITION of catalog primitives (@ax/ui Card /
 * Button / Input / ConfirmDialog + @ax/blocks AvatarGroup), not a redefined
 * primitive. Drives the live comment-thread endpoints: create / reply / edit /
 * delete. Soft-deleted comments arrive masked as "[deleted]" and render greyed.
 */
export function CommentThread({ entityType, entityId, currentUserId, enabled }: CommentThreadProps) {
  const comments = useComments(entityType, entityId, enabled);
  const create = useCreateComment(entityType, entityId);
  const edit = useEditComment(entityType, entityId);
  const remove = useDeleteComment(entityType, entityId);

  const [draft, setDraft] = useState('');
  const [replyTo, setReplyTo] = useState<string | null>(null);
  const [replyDraft, setReplyDraft] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editDraft, setEditDraft] = useState('');
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);

  const tree = useMemo(() => buildTree(comments.data?.items ?? []), [comments.data]);
  const total = comments.data?.totalElements ?? 0;

  const handleCreate = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    const body = draft.trim();
    if (!body) return;
    await create.mutateAsync({ entityType, entityId, body });
    setDraft('');
  };

  const handleReply = async (parentId: string): Promise<void> => {
    const body = replyDraft.trim();
    if (!body) return;
    await create.mutateAsync({ entityType, entityId, body, parentCommentId: parentId });
    setReplyDraft('');
    setReplyTo(null);
  };

  const handleEdit = async (id: string): Promise<void> => {
    const body = editDraft.trim();
    if (!body) return;
    await edit.mutateAsync({ id, body });
    setEditingId(null);
    setEditDraft('');
  };

  const renderComment = (comment: Comment, isReply: boolean): React.ReactElement => {
    const deleted = comment.status === 'DELETED' || comment.body === DELETED_BODY_MASK;
    const mine = currentUserId !== undefined && comment.authorUserId === currentUserId;
    const editing = editingId === comment.id;

    return (
      <div className={isReply ? 'ml-8 border-l border-border pl-4' : ''}>
        <div className="flex items-start gap-3">
          <AvatarGroup members={[{ name: displayName(comment.authorUserId, currentUserId) }]} label="작성자" />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
              <span className="text-sm font-semibold text-foreground">
                {displayName(comment.authorUserId, currentUserId)}
              </span>
              <span className="text-xs text-muted-foreground">{formatRelative(comment.createdAt)}</span>
              {comment.status === 'EDITED' ? (
                <span className="text-xs text-muted-foreground">(수정됨)</span>
              ) : null}
            </div>

            {editing ? (
              <div className="mt-2 flex items-center gap-2">
                <label htmlFor={`edit-${comment.id}`} className="sr-only">
                  댓글 수정
                </label>
                <Input
                  id={`edit-${comment.id}`}
                  value={editDraft}
                  onChange={(e) => setEditDraft(e.target.value)}
                  maxLength={4000}
                  className="flex-1"
                />
                <Button size="sm" loading={edit.isPending} onClick={() => handleEdit(comment.id)}>
                  저장
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setEditingId(null)}>
                  취소
                </Button>
              </div>
            ) : (
              <p
                className={
                  deleted
                    ? 'mt-1 text-sm italic text-muted-foreground'
                    : 'mt-1 whitespace-pre-wrap break-words text-sm text-foreground'
                }
              >
                {deleted ? '삭제된 댓글입니다' : comment.body}
              </p>
            )}

            {!deleted && !editing ? (
              <div className="mt-1.5 flex items-center gap-1">
                {!isReply ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => {
                      setReplyTo((curr) => (curr === comment.id ? null : comment.id));
                      setReplyDraft('');
                    }}
                  >
                    <CornerDownRight aria-hidden />
                    답글
                  </Button>
                ) : null}
                {mine ? (
                  <>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => {
                        setEditingId(comment.id);
                        setEditDraft(comment.body);
                      }}
                    >
                      <Pencil aria-hidden />
                      수정
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setPendingDelete(comment.id)}
                    >
                      <Trash2 aria-hidden />
                      삭제
                    </Button>
                  </>
                ) : null}
              </div>
            ) : null}

            {replyTo === comment.id ? (
              <div className="mt-2 flex items-center gap-2">
                <label htmlFor={`reply-${comment.id}`} className="sr-only">
                  답글 작성
                </label>
                <Input
                  id={`reply-${comment.id}`}
                  value={replyDraft}
                  onChange={(e) => setReplyDraft(e.target.value)}
                  placeholder="답글을 입력하세요"
                  maxLength={4000}
                  className="flex-1"
                />
                <Button
                  size="sm"
                  loading={create.isPending}
                  disabled={!replyDraft.trim()}
                  onClick={() => handleReply(comment.id)}
                >
                  등록
                </Button>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    );
  };

  return (
    <section aria-labelledby="comments-heading" className="space-y-4">
      <h2 id="comments-heading" className="text-lg font-semibold text-foreground">
        댓글 {total}
      </h2>

      <Card>
        <CardContent className="p-4">
          <form onSubmit={handleCreate} className="flex items-center gap-2">
            <label htmlFor="new-comment" className="sr-only">
              댓글 작성
            </label>
            <Input
              id="new-comment"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="따뜻한 댓글을 남겨보세요"
              maxLength={4000}
              className="flex-1"
            />
            <Button type="submit" loading={create.isPending && replyTo === null} disabled={!draft.trim()}>
              <Send aria-hidden />
              <span className="hidden sm:inline">등록</span>
            </Button>
          </form>
          {create.isError ? (
            <p role="alert" className="mt-2 text-xs font-medium text-destructive">
              {errorMessage(create.error)}
            </p>
          ) : null}
        </CardContent>
      </Card>

      {comments.isLoading ? (
        <LoadingState label="댓글 불러오는 중" />
      ) : comments.isError ? (
        <ErrorState message={errorMessage(comments.error)} onRetry={() => comments.refetch()} />
      ) : tree.length === 0 ? (
        <p className="py-6 text-center text-sm text-muted-foreground">
          첫 댓글을 남겨보세요.
        </p>
      ) : (
        <ul className="space-y-5">
          {tree.map((root) => (
            <li key={root.id} className="space-y-3">
              {renderComment(root, false)}
              {root.replies.map((reply) => (
                <div key={reply.id}>{renderComment(reply, true)}</div>
              ))}
            </li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        onOpenChange={(open) => {
          if (!open) setPendingDelete(null);
        }}
        title="댓글을 삭제할까요?"
        description="삭제한 댓글은 '삭제된 댓글입니다'로 표시됩니다."
        confirmLabel="삭제"
        tone="destructive"
        loading={remove.isPending}
        onConfirm={() => {
          if (pendingDelete) {
            remove.mutate(pendingDelete, { onSuccess: () => setPendingDelete(null) });
          }
        }}
      />
    </section>
  );
}
