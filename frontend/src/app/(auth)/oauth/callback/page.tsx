import React, { Suspense } from 'react';
import { OAuthCallbackClient } from './OAuthCallbackClient';
import { Spinner } from '@ax/ui';

function OAuthCallbackFallback() {
  return (
    <main className="ax-auth-backdrop ax-grain flex min-h-dvh items-center justify-center px-4">
      <Spinner className="h-6 w-6 text-muted-foreground" label="OAuth 로그인 처리 중" />
    </main>
  );
}

// Suspense boundary required because OAuthCallbackClient uses useSearchParams()
export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={<OAuthCallbackFallback />}>
      <OAuthCallbackClient />
    </Suspense>
  );
}
