import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../lib/auth/authStore';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login, isLoading, error } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch {}
  };

  return (
    <form onSubmit={handleSubmit}>
      <h1>Login</h1>
      {error && <p style={{color:'red'}}>{error}</p>}
      <div>
        <label>Email: <input type="email" value={email} onChange={e => setEmail(e.target.value)} required /></label>
      </div>
      <div>
        <label>Password: <input type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
      </div>
      <button type="submit" disabled={isLoading}>{isLoading ? 'Logging in...' : 'Login'}</button>
      <p><a href="/signup">Don't have an account? Sign up</a></p>
    </form>
  );
}
