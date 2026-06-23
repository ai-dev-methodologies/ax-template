import { Suspense } from 'react';
import { LoginScreen } from '@/features/auth/components';

export default function LoginPage() {
  return (
    <Suspense>
      <LoginScreen />
    </Suspense>
  );
}
