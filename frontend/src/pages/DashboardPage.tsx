import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../lib/auth/authStore';

export function DashboardPage() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!user) {
    return <div>Not logged in. <a href="/login">Login</a></div>;
  }

  const displayRole = user.roles && user.roles.length > 0 ? user.roles.join(', ') : 'User';
  const isVerified = user.verificationState === 'VERIFIED';

  return (
    <div>
      <h1>Dashboard</h1>
      <p>Welcome, {user.email}</p>
      <p>Role: {displayRole}</p>
      <p>Email verified: {isVerified ? 'Yes' : 'No'}</p>
      <button onClick={handleLogout}>Logout</button>
    </div>
  );
}
