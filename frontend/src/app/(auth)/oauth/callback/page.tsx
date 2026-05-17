import React, { Suspense } from 'react';
import { OAuthCallbackClient } from './OAuthCallbackClient';

// Suspense boundary required because OAuthCallbackClient uses useSearchParams()
export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={<p>OAuth 로그인 처리 중...</p>}>
      <OAuthCallbackClient />
    </Suspense>
  );
}
