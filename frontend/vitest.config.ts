import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
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
    ],
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
  },
});
