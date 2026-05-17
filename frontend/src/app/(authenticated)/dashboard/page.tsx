'use client';

import React, { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '../../../lib/auth/authStore';

export default function DashboardPage() {
  const { user, accessToken, logout, fetchMe } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    if (accessToken) fetchMe();
  }, [accessToken, fetchMe]);

  const handleLogout = async () => {
    await logout();
    router.push('/login');
  };

  // Inline guard removed — middleware.ts handles server-side redirect.
  // app/(authenticated)/layout.tsx handles client-side defensive check.

  return (
    <div style={{ maxWidth: 500, margin: '40px auto', fontFamily: 'sans-serif' }}>
      <h1>Dashboard</h1>
      {user ? (
        <div style={{ background: '#f8f9fa', padding: 20, borderRadius: 8 }}>
          <p><strong>이메일:</strong> {user.email}</p>
          <p><strong>역할:</strong> {user.roles?.join(', ')}</p>
          <p><strong>이메일 인증:</strong> {user.verificationState === 'verified' ? '완료' : '미완료'}</p>
          {user.providerLinks && user.providerLinks.length > 0 && (
            <p><strong>연결된 SNS:</strong> {user.providerLinks.map(p => p.provider).join(', ')}</p>
          )}
        </div>
      ) : (
        <p>프로필 로딩 중...</p>
      )}
      <button onClick={handleLogout}
        style={{ marginTop: 16, padding: '10px 20px', background: '#dc3545', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer' }}>
        로그아웃
      </button>
    </div>
  );
}
