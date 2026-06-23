import { Suspense } from 'react';
import { LoginScreen } from '@/features/auth/components';

export default function Page() {
  return (
    <Suspense>
      <LoginScreen />
    </Suspense>
  );
}
