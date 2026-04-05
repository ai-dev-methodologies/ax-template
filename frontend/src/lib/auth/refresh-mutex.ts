let refreshPromise: Promise<boolean> | null = null;

export async function tryRefresh(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;
  refreshPromise = doRefresh();
  try { return await refreshPromise; }
  finally { refreshPromise = null; }
}

async function doRefresh(): Promise<boolean> {
  try {
    const res = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' });
    if (!res.ok) return false;
    const data = await res.json();
    const { useAuthStore } = await import('./authStore');
    useAuthStore.setState({ accessToken: data.accessToken });
    return true;
  } catch { return false; }
}
