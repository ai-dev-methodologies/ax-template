import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      // Kit unit tests import repo-root templates/* hook files that statically
      // import next/navigation; vite can't resolve that bare specifier for a
      // file outside the project root. Point it at an inert stub. Component
      // tests that exercise the hooks vi.mock('next/navigation') over this.
      'next/navigation': path.resolve(__dirname, './tests/_stubs/next-navigation.ts'),
    },
  },
  test: {
    include: ['tests/**/*.vitest.{ts,tsx}', 'tests/**/*.spec.{ts,tsx}'],
    // Playwright tests use @playwright/test and must run under `npx playwright test`.
    // Excluding them from vitest prevents the "test.describe() not expected here" error.
    exclude: [
      'tests/L4/**',
      'tests/e2e-*.spec.ts',
      'tests/key-flow.spec.ts',
      'tests/auth/**',
      'tests/recipes/**',
    ],
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
  },
});
