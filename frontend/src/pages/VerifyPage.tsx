import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { authClient } from '../lib/api/authClient';

export function VerifyPage() {
  const [searchParams] = useSearchParams();
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
      .catch((err: any) => { setStatus('error'); setMessage(err.message || 'Verification failed.'); });
  }, [searchParams]);

  return (
    <div>
      <h1>Email Verification</h1>
      <p style={{color: status === 'success' ? 'green' : status === 'error' ? 'red' : 'black'}}>{message}</p>
      {status === 'success' && <a href="/login">Proceed to Login</a>}
    </div>
  );
}
