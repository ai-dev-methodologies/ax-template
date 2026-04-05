import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../lib/auth/authStore';

export function OAuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<'processing' | 'success' | 'error'>('processing');
  const [message, setMessage] = useState('OAuth 로그인 처리 중...');
  const navigate = useNavigate();

  useEffect(() => {
    const accessToken = searchParams.get('accessToken') || searchParams.get('access_token');
    const error = searchParams.get('error');

    if (error) {
      setStatus('error');
      setMessage(`OAuth 로그인 실패: ${error}`);
      return;
    }

    if (accessToken) {
      useAuthStore.setState({ accessToken });
      setStatus('success');
      setMessage('로그인 성공! 대시보드로 이동합니다...');
      setTimeout(() => navigate('/dashboard'), 1000);
    } else {
      setStatus('error');
      setMessage('OAuth 응답에 토큰이 없습니다. 서버 콜백 구현을 확인하세요.');
    }
  }, [searchParams, navigate]);

  return (
    <div style={{ maxWidth: 400, margin: '40px auto', fontFamily: 'sans-serif', textAlign: 'center' }}>
      <h2>OAuth 로그인</h2>
      <p style={{ color: status === 'success' ? 'green' : status === 'error' ? 'red' : '#333' }}>{message}</p>
      {status === 'error' && <a href="/login">로그인으로 돌아가기</a>}
    </div>
  );
}
