'use client';

import React from 'react';
import { ArticleEditor } from '@/features/articles/components';

/** New-article route. The editor handles create mode when no article is passed. */
export default function NewArticlePage() {
  return <ArticleEditor />;
}
