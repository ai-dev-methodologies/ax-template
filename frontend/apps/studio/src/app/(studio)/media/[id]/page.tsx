import { use } from 'react';
import { MediaDetailScreen } from '@/features/media/components';

export default function MediaDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <MediaDetailScreen id={id} />;
}
