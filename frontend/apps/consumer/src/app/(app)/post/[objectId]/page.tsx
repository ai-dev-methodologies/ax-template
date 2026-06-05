'use client';

import React from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { ArrowLeft, MessageCircle } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Card, CardContent, CardHeader, CardTitle } from '@ax/ui';
import { CommentThread } from '@/components/comment-thread';
import { FavoriteToggle } from '@/components/favorite-toggle';
import { useProfile } from '@/features/profile/hooks';

const ENTITY_TYPE = 'post';

export default function PostDetailPage() {
  const params = useParams<{ objectId: string }>();
  const objectId = decodeURIComponent(params.objectId);
  const { accessToken } = useAuthStore();
  const enabled = Boolean(accessToken);
  const profile = useProfile(enabled);

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" asChild>
        <Link href="/">
          <ArrowLeft aria-hidden />
          피드로 돌아가기
        </Link>
      </Button>

      <Card className="shadow-md">
        <CardHeader className="flex flex-row items-start justify-between gap-4">
          <div className="min-w-0">
            <CardTitle as="h1" className="flex items-center gap-2 text-xl">
              <MessageCircle aria-hidden className="h-5 w-5 text-[var(--ax-status-accent-fg)]" />
              게시물
            </CardTitle>
            <p className="mt-1 break-all font-mono text-xs text-muted-foreground">{objectId}</p>
          </div>
          <FavoriteToggle entityType={ENTITY_TYPE} entityId={objectId} enabled={enabled} />
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            이 게시물에 대한 대화에 참여해 보세요. 댓글을 남기고 답글로 이야기를 이어갈 수 있어요.
          </p>
        </CardContent>
      </Card>

      <CommentThread
        entityType={ENTITY_TYPE}
        entityId={objectId}
        currentUserId={profile.data?.userId}
        enabled={enabled}
      />
    </div>
  );
}
