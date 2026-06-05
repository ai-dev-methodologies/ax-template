import React, { Suspense } from 'react';
import { VerifyPageClient } from './VerifyPageClient';
import { Spinner } from '@/components/ui/spinner';

function VerifyFallback() {
  return (
    <main className="ax-auth-backdrop ax-grain flex min-h-dvh items-center justify-center px-4">
      <Spinner className="h-6 w-6 text-muted-foreground" label="인증 중" />
    </main>
  );
}

// Suspense boundary required because VerifyPageClient uses useSearchParams().
export default function VerifyPage() {
  return (
    <Suspense fallback={<VerifyFallback />}>
      <VerifyPageClient />
    </Suspense>
  );
}
