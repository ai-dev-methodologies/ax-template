'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Check, ImagePlus, Loader2, Tag as TagIcon, Trash2, X } from 'lucide-react';
import { Alert, Button, Card, CardContent, ConfirmDialog, Label, cn } from '@ax/ui';
import { RichTextEditor } from '@ax/blocks';
import {
  useCreateArticle,
  useDeleteArticle,
  useUpdateArticle,
} from '@/features/articles/hooks';
import {
  useArticleTags,
  useAttachTag,
  useDetachTag,
  useTags,
} from '@/features/tags/hooks';
import { useUploadCover } from '@/features/files/hooks';
import { composeBody, extractCover, stripCover } from '@/lib/article-body';
import { errorMessage } from '@/lib/errors';
import type { Article } from '@/lib/api/articleClient';

interface ArticleEditorProps {
  /** Existing article for edit mode; absent => create mode. */
  article?: Article;
}

/**
 * Article authoring surface — composed entirely from catalog pieces:
 *   - the SERIF title input is a token-styled native <input> (NOT a redefined
 *     catalog Input — the boundary rule forbids an app-local `Input`); it binds
 *     to the same --ring / border / font-display tokens.
 *   - the body is the @ax/blocks RichTextEditor (Tiptap StarterKit).
 *   - tag assignment uses the @ax/ui pieces + the tag domain hooks.
 *   - the cover upload posts to file-storage and embeds the cover into the body.
 *   - Save -> crud create/update (which also re-indexes the article for search).
 */
export function ArticleEditor({ article }: ArticleEditorProps) {
  const router = useRouter();
  const isEdit = Boolean(article);
  const articleId = article?.id;

  const [title, setTitle] = useState(article?.title ?? '');
  const [body, setBody] = useState(() => stripCover(article?.description));
  const [coverUrl, setCoverUrl] = useState<string | null>(() =>
    extractCover(article?.description),
  );
  const [titleError, setTitleError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const create = useCreateArticle();
  const update = useUpdateArticle(articleId ?? '');
  const remove = useDeleteArticle();
  const uploadCover = useUploadCover();

  const tags = useTags();
  const articleTags = useArticleTags(articleId);
  const attach = useAttachTag(articleId ?? '');
  const detach = useDetachTag(articleId ?? '');

  const fileInputRef = useRef<HTMLInputElement>(null);

  const saving = create.isPending || update.isPending;
  const saveError = create.error ?? update.error;

  // Keep the title error cleared as the editor types.
  useEffect(() => {
    if (title.trim()) setTitleError(null);
  }, [title]);

  const handleCoverPick = async (e: React.ChangeEvent<HTMLInputElement>): Promise<void> => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      const stored = await uploadCover.mutateAsync(file);
      setCoverUrl(stored.downloadUrl);
    } catch {
      // surfaced via uploadCover.error
    }
  };

  const handleSave = async (): Promise<void> => {
    if (!title.trim()) {
      setTitleError('제목을 입력하세요.');
      return;
    }
    const payload = { title: title.trim(), description: composeBody(body, coverUrl) };
    try {
      if (isEdit && articleId) {
        await update.mutateAsync(payload);
        router.push(`/article/${articleId}`);
      } else {
        const created = await create.mutateAsync(payload);
        router.push(`/article/${created.id}`);
      }
    } catch {
      // surfaced via saveError
    }
  };

  const handleDelete = async (): Promise<void> => {
    if (!articleId) return;
    try {
      await remove.mutateAsync(articleId);
      setConfirmOpen(false);
      router.push('/');
    } catch {
      // surfaced via remove.error (kept open so the editor can retry)
    }
  };

  const assigned = articleTags.data?.items ?? [];
  const assignedIds = new Set(assigned.map((t) => t.id));
  const allTags = tags.data?.items ?? [];

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-foreground pb-5">
        <p className="text-xs uppercase tracking-[0.22em] text-muted-foreground">
          {isEdit ? '글 편집' : '새 글'}
        </p>
        <div className="flex items-center gap-2">
          {isEdit && articleId ? (
            <>
              <Button
                variant="destructive"
                size="sm"
                disabled={remove.isPending}
                onClick={() => setConfirmOpen(true)}
              >
                <Trash2 aria-hidden />삭제
              </Button>
              <ConfirmDialog
                open={confirmOpen}
                onOpenChange={setConfirmOpen}
                title="이 글을 삭제할까요?"
                description="삭제하면 되돌릴 수 없습니다. (이 도메인에는 휴지통이 없습니다.)"
                confirmLabel="삭제"
                cancelLabel="취소"
                tone="destructive"
                loading={remove.isPending}
                onConfirm={handleDelete}
              />
            </>
          ) : null}
          <Button onClick={handleSave} loading={saving} size="sm">
            <Check aria-hidden />
            {isEdit ? '변경 사항 저장' : '발행'}
          </Button>
        </div>
      </header>

      {saveError ? <Alert variant="error">{errorMessage(saveError)}</Alert> : null}

      {/* Title — a serif, large-scale editorial headline input. */}
      <div className="space-y-1.5">
        <Label htmlFor="article-title">제목</Label>
        <input
          id="article-title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="헤드라인을 입력하세요"
          aria-invalid={titleError ? true : undefined}
          aria-describedby={titleError ? 'article-title-error' : undefined}
          className={cn(
            'w-full border-0 border-b border-border bg-transparent pb-3 font-display text-4xl font-bold leading-tight tracking-tight text-foreground outline-none transition-colors sm:text-5xl',
            'placeholder:text-muted-foreground/40',
            'focus-visible:border-foreground',
            titleError && 'border-destructive',
          )}
        />
        {titleError ? (
          <p id="article-title-error" role="alert" className="text-xs font-medium text-destructive">
            {titleError}
          </p>
        ) : null}
      </div>

      {/* Cover image. */}
      <div className="space-y-2">
        <Label htmlFor="cover-input">표지 이미지</Label>
        <input
          ref={fileInputRef}
          id="cover-input"
          type="file"
          accept="image/png,image/jpeg,image/webp,image/gif"
          onChange={handleCoverPick}
          className="sr-only"
        />
        {coverUrl ? (
          <div className="relative w-full max-w-xl border border-border">
            {/* Cover is a relative /api/files URL via the proxy; native <img> is
                intentional (no Next Image optimizer in this app). */}
            <img src={coverUrl} alt="표지 미리보기" className="aspect-[3/2] w-full object-cover" />
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setCoverUrl(null)}
              className="absolute right-2 top-2"
            >
              <X aria-hidden />표지 제거
            </Button>
          </div>
        ) : (
          <Button
            type="button"
            variant="outline"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadCover.isPending}
          >
            {uploadCover.isPending ? (
              <Loader2 aria-hidden className="animate-spin motion-reduce:animate-none" />
            ) : (
              <ImagePlus aria-hidden />
            )}
            {uploadCover.isPending ? '업로드 중' : '표지 업로드'}
          </Button>
        )}
        {uploadCover.error ? (
          <p role="alert" className="text-xs font-medium text-destructive">
            {errorMessage(uploadCover.error)}
          </p>
        ) : null}
      </div>

      {/* Body — the catalog RichTextEditor. The editable region carries its own
          aria-label ("기사 본문"), so this is a visible caption, not a form label. */}
      <div className="space-y-2">
        <span className="text-sm font-medium leading-none text-foreground">본문</span>
        <RichTextEditor
          value={article ? stripCover(article.description) : ''}
          onChange={setBody}
          ariaLabel="기사 본문"
          placeholder="이야기를 시작하세요…"
        />
      </div>

      {/* Tag assignment — only in edit mode (an article must exist to attach to). */}
      <Card>
        <CardContent className="space-y-3 py-5">
          <div className="flex items-center gap-2">
            <TagIcon aria-hidden className="h-4 w-4 text-muted-foreground" />
            <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-foreground">태그</h2>
          </div>
          {!isEdit ? (
            <p className="text-sm text-muted-foreground">
              글을 먼저 발행하면 태그를 지정할 수 있습니다.
            </p>
          ) : allTags.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              정의된 태그가 없습니다. 태그 화면에서 먼저 만들어 주세요.
            </p>
          ) : (
            <div className="flex flex-wrap gap-2" role="group" aria-label="태그 지정">
              {allTags.map((tag) => {
                const on = assignedIds.has(tag.id);
                const pending = attach.isPending || detach.isPending;
                return (
                  <button
                    key={tag.id}
                    type="button"
                    aria-pressed={on}
                    disabled={pending}
                    onClick={() =>
                      on ? detach.mutate(tag.id) : attach.mutate(tag.id)
                    }
                    className={cn(
                      'inline-flex items-center gap-1.5 border px-3 py-1 text-xs uppercase tracking-[0.06em] transition-colors disabled:opacity-50',
                      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
                      on
                        ? 'border-foreground bg-foreground text-background'
                        : 'border-border text-muted-foreground hover:border-foreground hover:text-foreground',
                    )}
                  >
                    {on ? <Check aria-hidden className="h-3 w-3" /> : <TagIcon aria-hidden className="h-3 w-3" />}
                    {tag.name}
                  </button>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
