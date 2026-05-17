'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '../../../lib/auth/authStore';

const API_BASE = '/api';
const OAUTH_PROVIDERS = [
  { id: 'google', label: 'Google 로그인', color: '#4285F4' },
  { id: 'kakao', label: 'Kakao 로그인', color: '#FEE500', textColor: '#000' },
  { id: 'naver', label: 'Naver 로그인', color: '#03C75A' },
];

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login, isLoading, error } = useAuthStore();
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(email, password);
      router.push('/dashboard');
    } catch {}
  };

  const handleOAuth = (provider: string) => {
    window.location.href = `${API_BASE}/auth/oauth/${provider}/authorize`;
  };

  return (
    <div style={{ maxWidth: 400, margin: '40px auto', fontFamily: 'sans-serif' }}>
      <h1>Login</h1>
      {error && <p style={{ color: 'red', padding: 8, background: '#fee', borderRadius: 4 }}>{error}</p>}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 24 }}>
        {OAUTH_PROVIDERS.map(p => (
          <button
            key={p.id}
            onClick={() => handleOAuth(p.id)}
            style={{
              padding: '12px 16px', border: 'none', borderRadius: 6, cursor: 'pointer',
              fontSize: 14, fontWeight: 600, color: p.textColor || '#fff', background: p.color,
            }}
          >
            {p.label}
          </button>
        ))}
      </div>

      <hr style={{ margin: '16px 0', border: 'none', borderTop: '1px solid #ddd' }} />
      <p style={{ textAlign: 'center', color: '#888', fontSize: 13 }}>또는 이메일로 로그인</p>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: 'block', marginBottom: 4, fontSize: 13 }}>이메일</label>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
            style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc', boxSizing: 'border-box' }} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: 'block', marginBottom: 4, fontSize: 13 }}>비밀번호</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required
            style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc', boxSizing: 'border-box' }} />
        </div>
        <button type="submit" disabled={isLoading}
          style={{ width: '100%', padding: 12, background: '#333', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 14 }}>
          {isLoading ? '로그인 중...' : '이메일 로그인'}
        </button>
      </form>
      <p style={{ textAlign: 'center', marginTop: 16, fontSize: 13 }}>
        <Link href="/signup">계정이 없으신가요? 회원가입</Link>
      </p>
    </div>
  );
}
