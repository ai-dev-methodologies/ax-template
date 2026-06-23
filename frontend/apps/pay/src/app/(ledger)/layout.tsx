import React from 'react';
import { LedgerLayoutScreen } from '@/features/shell/components';

export default function LedgerLayout({ children }: { children: React.ReactNode }) {
  return <LedgerLayoutScreen>{children}</LedgerLayoutScreen>;
}
