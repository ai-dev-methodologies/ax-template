/**
 * MSW browser worker initializer — dev mode only.
 * Called from src/components/providers.tsx via useEffect on mount.
 * Safe to import in server components (returns early when typeof window === 'undefined').
 */
export async function initMocks(): Promise<void> {
  if (typeof window === 'undefined') return;
  if (process.env.NODE_ENV !== 'development') return;
  const { worker } = await import('./browser');
  await worker.start({ onUnhandledRequest: 'bypass' });
}
