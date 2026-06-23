import React from 'react';
import { StudioShell } from '@/features/shell/components';

export default function StudioLayout({ children }: { children: React.ReactNode }) {
  return <StudioShell>{children}</StudioShell>;
}
