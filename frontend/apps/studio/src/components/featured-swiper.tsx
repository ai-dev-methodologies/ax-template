'use client';

import React, { useEffect, useState } from 'react';
import { ImageSwiper } from '@ax/blocks';
import { Spinner } from '@ax/ui';
import { fileClient, type StoredFile } from '@/lib/api/fileClient';
import { isImageContentType } from '@/lib/format';

interface FeaturedSwiperProps {
  /** READY image files to feature (the component resolves authed object URLs). */
  files: StoredFile[];
  /** how many to feature at most */
  max?: number;
}

/**
 * A swipe-through showcase of the creator's featured media, composing the
 * catalog ImageSwiper. ImageSwiper renders plain <img src> strings, but the
 * studio's download endpoint needs the JWT — so this resolves each featured
 * file to an authed blob object URL first, then hands the URL strings to the
 * block. Object URLs are revoked on unmount to avoid leaks. Domain component,
 * not a catalog primitive.
 */
export function FeaturedSwiper({ files, max = 5 }: FeaturedSwiperProps) {
  const featured = files
    .filter((f) => f.status === 'READY' && isImageContentType(f.contentType))
    .slice(0, max);
  const ids = featured.map((f) => f.id).join(',');

  const [urls, setUrls] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const created: string[] = [];
    setLoading(true);
    setUrls([]);
    if (featured.length === 0) {
      setLoading(false);
      return;
    }
    Promise.all(
      featured.map((f) =>
        fileClient
          .download(f.id)
          .then((blob) => URL.createObjectURL(blob))
          .catch(() => null),
      ),
    ).then((results) => {
      if (cancelled) {
        results.forEach((u) => u && URL.revokeObjectURL(u));
        return;
      }
      const ok = results.filter((u): u is string => Boolean(u));
      created.push(...ok);
      setUrls(ok);
      setLoading(false);
    });
    return () => {
      cancelled = true;
      created.forEach((u) => URL.revokeObjectURL(u));
    };
    // `ids` is the stable joined identity of the featured set; `featured` is
    // recomputed from it each render, so it is intentionally not a dep here.
  }, [ids]);

  if (featured.length === 0) return null;

  if (loading || urls.length === 0) {
    return (
      <div
        className="grid aspect-[16/9] w-full place-items-center rounded-[var(--radius)] border border-border bg-card shadow-md"
        role="status"
        aria-live="polite"
      >
        <Spinner className="h-7 w-7 text-[var(--ax-status-accent-fg)]" label="추천 작품 불러오는 중" />
      </div>
    );
  }

  return (
    <div
      className="aspect-[16/9] w-full overflow-hidden rounded-[var(--radius)] border border-border bg-card shadow-lg"
      aria-label="추천 작품 스와이프 쇼케이스"
      role="group"
    >
      <ImageSwiper images={urls} className="rounded-[var(--radius)]" />
    </div>
  );
}
