import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../lib/auth/authStore';

export function SignupPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [done, setDone] = useState(false);
  const { signup, isLoading, error } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < 12) {
      alert('Password must be at least 12 characters');
      return;
    }
    try {
      await signup(email, password);
      setDone(true);
    } catch {}
  };

  if (done) return <div>Check your email for a verification link. <a href="/login">Login</a></div>;

  return (
    <form onSubmit={handleSubmit}>
      <h1>Sign Up</h1>
      {error && <p style={{color:'red'}}>{error}</p>}
      <div>
        <label>Email: <input type="email" value={email} onChange={e => setEmail(e.target.value)} required /></label>
      </div>
      <div>
        <label>Password (min 12 chars): <input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={12} maxLength={128} /></label>
      </div>
      <button type="submit" disabled={isLoading}>{isLoading ? 'Signing up...' : 'Sign Up'}</button>
      <p><a href="/login">Already have an account? Login</a></p>
    </form>
  );
}
