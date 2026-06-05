/**
 * MSW browser worker initializer — dev mode only.
 * Called from src/components/providers.tsx via useEffect on mount.
 * Safe to import in server components (returns early when typeof window === 'undefined').
 */
export async function initMocks(): Promise<void> {
  if (typeof window === 'undefined') return;
  if (process.env.NODE_ENV !== 'development') return;
  // OPT-IN only: by default `npm run dev` hits the REAL backend (via the next.config.ts /api proxy
  // to :8080) so end-to-end testing exercises the live Spring services. Set
  // NEXT_PUBLIC_ENABLE_MSW=true to mock /api/auth/* for backend-less UI work.
  if (process.env.NEXT_PUBLIC_ENABLE_MSW !== 'true') return;
  const { worker } = await import('./browser');
  await worker.start({ onUnhandledRequest: 'bypass' });
}
