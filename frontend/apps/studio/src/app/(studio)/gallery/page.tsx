'use client';

import React from 'react';
import Link from 'next/link';
import { ImageOff, Upload } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button } from '@ax/ui';
import { PageHeader } from '@/components/page-header';
import { MediaCard } from '@/features/media/components';
import { FeaturedSwiper } from '@/components/featured-swiper';
import { ScreenEmpty, ScreenError, ScreenLoading } from '@/components/screen-states';
import { useMediaIndex, useMediaList } from '@/features/media/hooks';
import { errorMessage } from '@/lib/errors';

/**
 * Gallery — the creator's full media grid. The index comes from the favorites
 * domain (entityType "file"); each id resolves to metadata in parallel. Loading
 * and empty states are handled explicitly. The grid cascades in via the persona
 * stagger (each card sets --ax-i).
 */
export default function GalleryPage() {
  const enabled = Boolean(useAuthStore((s) => s.accessToken));
  const index = useMediaIndex(enabled);
  const ids = index.data ?? [];
  const { files, isLoading: filesLoading, isError: filesError } = useMediaList(ids, enabled);

  return (
    <section aria-labelledby="gallery-heading">
      <span id="gallery-heading" className="sr-only">
        갤러리
      </span>
      <PageHeader
        title="갤러리"
        description="업로드한 모든 작품을 한 눈에. 카드를 눌러 미리보기와 컬렉션, 좋아요를 확인하세요."
        action={
          <Button asChild size="lg">
            <Link href="/upload">
              <Upload aria-hidden />
              업로드
            </Link>
          </Button>
        }
      />

      {index.isLoading ? (
        <ScreenLoading label="갤러리 불러오는 중" />
      ) : index.isError ? (
        <ScreenError error={new Error(errorMessage(index.error))} />
      ) : ids.length === 0 ? (
        <ScreenEmpty
          icon={<ImageOff className="h-10 w-10" />}
          title="아직 작품이 없어요"
          description="첫 작품을 올리고 갤러리를 채워보세요."
          action={
            <Button asChild size="lg">
              <Link href="/upload">
                <Upload aria-hidden />첫 업로드
              </Link>
            </Button>
          }
        />
      ) : filesLoading && files.length === 0 ? (
        <ScreenLoading label="미디어 불러오는 중" />
      ) : filesError ? (
        <ScreenError error={new Error('미디어를 불러오지 못했어요.')} />
      ) : (
        <div className="space-y-8">
          {/* Featured swipe showcase — the catalog ImageSwiper over authed media. */}
          <FeaturedSwiper files={files} />

          <ul className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {files.map((file, i) => (
              <li key={file.id}>
                <MediaCard file={file} index={i} />
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
