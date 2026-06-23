'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Heart } from 'lucide-react';
import { Button, cn } from '@ax/ui';
import {
  useAddFavorite,
  useFavoriteCount,
  useIsFavorited,
  useRemoveFavorite,
} from '@/features/favorites/hooks';
import { FILE_ENTITY_TYPE } from '@/features/media/hooks';

interface ReactionButtonProps {
  entityId: string;
  enabled: boolean;
  /** display name stored as the favorite note when liking */
  note?: string;
}

/**
 * The studio's signature delight: a like/reaction toggle on a media item. Reads
 * the live favorited state + global like count, flips it via the favorites
 * endpoints, and fires a compositor-friendly "pop" burst on like (the
 * .ax-pop-on keyframe, fully neutralized under prefers-reduced-motion). The
 * control is a real toggle button (aria-pressed) so assistive tech announces the
 * state; the count has an accessible label too.
 *
 * Composes the catalog Button + cn + the favorites hooks. Domain component, not
 * a catalog primitive.
 */
export function ReactionButton({ entityId, enabled, note }: ReactionButtonProps) {
  const status = useIsFavorited(FILE_ENTITY_TYPE, entityId, enabled);
  const countQuery = useFavoriteCount(FILE_ENTITY_TYPE, entityId, enabled);
  const add = useAddFavorite();
  const remove = useRemoveFavorite();

  const favorited = status.data?.favorited ?? false;
  const count = countQuery.data?.count ?? 0;
  const pending = add.isPending || remove.isPending;

  // Replay the pop only when transitioning into the liked state.
  const [popping, setPopping] = useState(false);
  const heartRef = useRef<HTMLSpanElement>(null);
  const prevFavorited = useRef(favorited);
  useEffect(() => {
    if (favorited && !prevFavorited.current) {
      setPopping(true);
      const timer = setTimeout(() => setPopping(false), 480);
      prevFavorited.current = favorited;
      return () => clearTimeout(timer);
    }
    prevFavorited.current = favorited;
  }, [favorited]);

  const handleToggle = (): void => {
    if (favorited) {
      remove.mutate({ entityType: FILE_ENTITY_TYPE, entityId });
    } else {
      add.mutate({ entityType: FILE_ENTITY_TYPE, entityId, note });
    }
  };

  return (
    <Button
      variant={favorited ? 'default' : 'outline'}
      size="lg"
      aria-pressed={favorited}
      aria-label={favorited ? '좋아요 취소' : '좋아요'}
      loading={pending}
      onClick={handleToggle}
    >
      {!pending ? (
        <span ref={heartRef} className={cn('inline-flex', popping && 'ax-pop-on')}>
          <Heart
            aria-hidden
            className={cn('transition-colors', favorited && 'fill-current')}
          />
        </span>
      ) : null}
      <span>{favorited ? '좋아요됨' : '좋아요'}</span>
      <span
        className="rounded-full bg-foreground/10 px-2 py-0.5 text-xs font-bold tabular-nums"
        aria-label={`좋아요 ${count}개`}
      >
        {count}
      </span>
    </Button>
  );
}
