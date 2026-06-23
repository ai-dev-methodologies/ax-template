import { ConsoleShell } from '@/features/shell/components';

export default function Layout({ children }: { children: React.ReactNode }) {
  return <ConsoleShell>{children}</ConsoleShell>;
}
