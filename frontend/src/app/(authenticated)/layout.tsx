'use client';

import React, { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '../../lib/auth/authStore';

interface AuthenticatedLayoutProps {
  children: React.ReactNode;
}

/**
 * Authenticated group layout — defensive client-side auth check.
 * Primary guard is middleware.ts (server-side cookie check).
 * This handles the edge case where cookie passed middleware but Zustand store
 * (in-memory) says logged out after hydration.
 */
export default function AuthenticatedLayout({ children }: AuthenticatedLayoutProps) {
  const { accessToken } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    if (!accessToken) {
      router.replace('/login');
    }
  }, [accessToken, router]);

  if (!accessToken) return null;

  return <>{children}</>;
}
