import { ArticleReadScreen } from '@/features/articles/components';

export default function ArticleReadPage({ params }: { params: Promise<{ id: string }> }) {
  return <ArticleReadScreen params={params} />;
}
