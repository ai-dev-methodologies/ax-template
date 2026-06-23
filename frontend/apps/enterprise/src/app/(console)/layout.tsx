import React from 'react';
import { ConsoleLayoutScreen } from '@/features/shell/components';

export default function ConsoleLayout({ children }: { children: React.ReactNode }) {
  return <ConsoleLayoutScreen>{children}</ConsoleLayoutScreen>;
}
