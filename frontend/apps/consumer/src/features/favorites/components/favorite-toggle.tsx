'use client';

import React from 'react';
import { Heart } from 'lucide-react';
import { Button, cn } from '@ax/ui';
import { useAddFavorite, useIsFavorited, useRemoveFavorite } from '@/features/favorites/hooks';

interface FavoriteToggleProps {
  entityType: string;
  entityId: string;
  enabled: boolean;
  /** Optional note stored with the favorite when adding. */
  note?: string;
}

/**
 * Heart toggle — composes the catalog Button + cn. Reads the live favorited
 * state and flips it via the favorites endpoints (add / remove). The control is
 * a real toggle button (aria-pressed) so assistive tech announces the state.
 */
export function FavoriteToggle({ entityType, entityId, enabled, note }: FavoriteToggleProps) {
  const status = useIsFavorited(entityType, entityId, enabled);
  const add = useAddFavorite();
  const remove = useRemoveFavorite();

  const favorited = status.data?.favorited ?? false;
  const pending = add.isPending || remove.isPending;

  const handleToggle = (): void => {
    if (favorited) {
      remove.mutate({ entityType, entityId });
    } else {
      add.mutate({ entityType, entityId, note });
    }
  };

  return (
    <Button
      variant="outline"
      size="sm"
      aria-pressed={favorited}
      aria-label={favorited ? '즐겨찾기 해제' : '즐겨찾기 추가'}
      loading={pending}
      onClick={handleToggle}
    >
      {!pending ? (
        <Heart
          aria-hidden
          className={cn('transition-colors', favorited && 'fill-current text-[var(--ax-status-danger-fg)]')}
        />
      ) : null}
      {favorited ? '즐겨찾기됨' : '즐겨찾기'}
    </Button>
  );
}
