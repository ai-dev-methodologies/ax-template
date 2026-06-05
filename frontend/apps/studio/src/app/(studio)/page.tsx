'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import { ImageOff, Upload } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button } from '@ax/ui';
import { ImageCarouselHero } from '@ax/blocks';
import { MediaThumb } from '@/components/media-thumb';
import { staggerClass } from '@/components/media-card';
import { ScreenEmpty, ScreenLoading } from '@/components/screen-states';
import { useMediaIndex, useMediaList } from '@/features/media/hooks';

/**
 * On-theme decorative gradient cards for the cinematic hero carousel. These are
 * pure inline-SVG data URIs (zero network, no WebGL) — a perf-conscious choice
 * for the most motion-heavy persona: the ONE hero animates its rotating cards
 * from these lightweight gradients, while real uploaded media (authed blobs)
 * lives in the swiper + recent strip below. Each carries descriptive alt text.
 */
const HERO_GRADIENTS: { id: string; from: string; to: string; alt: string }[] = [
  { id: 'g1', from: '#ff3da6', to: '#ff9d3d', alt: '마젠타에서 탠저린으로 흐르는 그라데이션' },
  { id: 'g2', from: '#7c3aed', to: '#22d3ee', alt: '보라에서 시안으로 흐르는 그라데이션' },
  { id: 'g3', from: '#f43f5e', to: '#fbbf24', alt: '로즈에서 골드로 흐르는 그라데이션' },
  { id: 'g4', from: '#06b6d4', to: '#a3e635', alt: '청록에서 라임으로 흐르는 그라데이션' },
  { id: 'g5', from: '#8b5cf6', to: '#ec4899', alt: '바이올렛에서 핑크로 흐르는 그라데이션' },
];

function gradientDataUri(from: string, to: string): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="320" height="400"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${from}"/><stop offset="1" stop-color="${to}"/></linearGradient></defs><rect width="320" height="400" fill="url(#g)"/></svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

/**
 * Studio home — a cinematic hero (the ONE WebGL-free hero block) over a strip of
 * recent uploads. The hero composes the catalog ImageCarouselHero; the recent
 * strip composes the catalog ImageSwiper of real authed media thumbnails.
 */
export default function StudioHomePage() {
  const router = useRouter();
  const enabled = Boolean(useAuthStore((s) => s.accessToken));
  const index = useMediaIndex(enabled);
  const ids = (index.data ?? []).slice(0, 6);
  const { files, isLoading: filesLoading } = useMediaList(ids, enabled);

  const heroImages = HERO_GRADIENTS.map((g, i) => ({
    id: g.id,
    src: gradientDataUri(g.from, g.to),
    alt: g.alt,
    rotation: (i % 2 === 0 ? -1 : 1) * (4 + i * 2),
  }));

  return (
    <div className="space-y-12">
      {/* Cinematic hero — one hero only (perf). */}
      <section
        aria-label="스튜디오 소개"
        className="overflow-hidden rounded-[var(--radius)] border border-border bg-card shadow-lg"
      >
        <ImageCarouselHero
          title="당신의 작업실, ax 스튜디오"
          subtitle="크리에이티브 스튜디오"
          description="작품을 올리고, 컬렉션으로 묶고, 좋아요로 반응하세요. 모든 미디어가 한 곳에서 살아 움직입니다."
          ctaText="작품 올리기"
          onCtaClick={() => router.push('/upload')}
          images={heroImages}
          features={[
            { title: '간편한 업로드', description: '드래그 한 번으로 작품을 스튜디오에 보관하세요.' },
            { title: '컬렉션 정리', description: '태그로 작품을 컬렉션으로 묶어 정리하세요.' },
            { title: '반응과 활동', description: '좋아요로 반응하고 활동 피드로 흐름을 따라가세요.' },
          ]}
        />
      </section>

      {/* Recent uploads. */}
      <section aria-labelledby="recent-heading" className="space-y-5">
        <div className="flex items-end justify-between gap-4">
          <h2 className="ax-display text-2xl font-extrabold tracking-tight text-foreground">
            최근 업로드
          </h2>
          <Button asChild variant="outline" size="sm">
            <a href="/gallery">갤러리 전체 보기</a>
          </Button>
        </div>

        {index.isLoading || (filesLoading && files.length === 0) ? (
          <ScreenLoading label="최근 작품 불러오는 중" />
        ) : files.length === 0 ? (
          <ScreenEmpty
            icon={<ImageOff className="h-10 w-10" />}
            title="아직 작품이 없어요"
            description="첫 작품을 올려 스튜디오를 시작하세요."
            action={
              <Button asChild size="lg">
                <a href="/upload">
                  <Upload aria-hidden />첫 업로드
                </a>
              </Button>
            }
          />
        ) : (
          <>
            {/* Swipe-through showcase of recent media (catalog ImageSwiper is
                designed for object-URL strings; we keep the real authed
                thumbnails as cards in the strip below for reliable previews). */}
            <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
              {files.map((file, i) => (
                <li
                  key={file.id}
                  className={`ax-stagger ax-float overflow-hidden rounded-[var(--radius)] border border-border bg-card shadow-md ${staggerClass(i)}`}
                >
                  <a
                    href={`/media/${file.id}`}
                    className="block focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
                    aria-label={`${file.fileName} 자세히 보기`}
                  >
                    <div className="aspect-square w-full">
                      <MediaThumb
                        id={file.id}
                        fileName={file.fileName}
                        contentType={file.contentType}
                        ready={file.status === 'READY'}
                        className="h-full w-full"
                      />
                    </div>
                  </a>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>
    </div>
  );
}
