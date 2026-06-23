import React from 'react';
import { StudioLayoutScreen } from '@/features/shell/components';

export default function StudioLayout({ children }: { children: React.ReactNode }) {
  return <StudioLayoutScreen>{children}</StudioLayoutScreen>;
}
