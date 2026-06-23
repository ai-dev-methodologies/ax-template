import type { ReactNode } from 'react';
import { AppShellScreen } from '@/features/shell/components';

export default function AppLayout({ children }: { children: ReactNode }) {
  return <AppShellScreen>{children}</AppShellScreen>;
}
