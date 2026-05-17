'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { authClient } from '../../../lib/api/authClient';

export function VerifyPageClient() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<'pending' | 'success' | 'error'>('pending');
  const [message, setMessage] = useState('Verifying...');

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setStatus('error');
      setMessage('No verification token provided.');
      return;
    }
    authClient.verifyEmail({ token })
      .then(res => { setStatus('success'); setMessage(res.message); })
      .catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : 'Verification failed.';
        setStatus('error');
        setMessage(msg);
      });
  }, [searchParams]);

  return (
    <div>
      <h1>Email Verification</h1>
      <p style={{ color: status === 'success' ? 'green' : status === 'error' ? 'red' : 'black' }}>{message}</p>
      {status === 'success' && <Link href="/login">Proceed to Login</Link>}
    </div>
  );
}
