'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useAuthStore } from '../../../lib/auth/authStore';

export default function SignupPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [done, setDone] = useState(false);
  const { signup, isLoading, error } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < 12) { alert('비밀번호는 12자 이상이어야 합니다'); return; }
    try { await signup(email, password); setDone(true); } catch {}
  };

  if (done) return (
    <div style={{ maxWidth: 400, margin: '40px auto', fontFamily: 'sans-serif', textAlign: 'center' }}>
      <h2>이메일을 확인하세요</h2>
      <p>인증 메일이 발송되었습니다. (개발 환경: 서버 콘솔 로그 확인)</p>
      <Link href="/login">로그인 →</Link>
    </div>
  );

  return (
    <div style={{ maxWidth: 400, margin: '40px auto', fontFamily: 'sans-serif' }}>
      <h1>회원가입</h1>
      {error && <p style={{ color: 'red', padding: 8, background: '#fee', borderRadius: 4 }}>{error}</p>}
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: 'block', marginBottom: 4, fontSize: 13 }}>이메일</label>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
            style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc', boxSizing: 'border-box' }} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: 'block', marginBottom: 4, fontSize: 13 }}>비밀번호 (12자 이상)</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={12} maxLength={128}
            style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc', boxSizing: 'border-box' }} />
        </div>
        <button type="submit" disabled={isLoading}
          style={{ width: '100%', padding: 12, background: '#333', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 14 }}>
          {isLoading ? '가입 중...' : '회원가입'}
        </button>
      </form>
      <p style={{ textAlign: 'center', marginTop: 16, fontSize: 13 }}>
        <Link href="/login">이미 계정이 있으신가요? 로그인</Link>
      </p>
    </div>
  );
}
