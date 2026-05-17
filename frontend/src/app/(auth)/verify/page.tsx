import React, { Suspense } from 'react';
import { VerifyPageClient } from './VerifyPageClient';

// Suspense boundary required because VerifyPageClient uses useSearchParams()
// Next.js App Router: components using useSearchParams must be wrapped in Suspense
export default function VerifyPage() {
  return (
    <Suspense fallback={<p>Verifying...</p>}>
      <VerifyPageClient />
    </Suspense>
  );
}
