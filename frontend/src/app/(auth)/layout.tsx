import React from 'react';

interface AuthLayoutProps {
  children: React.ReactNode;
}

// Auth group layout — unauthenticated shell (no sidebar, no nav)
export default function AuthLayout({ children }: AuthLayoutProps) {
  return <>{children}</>;
}
