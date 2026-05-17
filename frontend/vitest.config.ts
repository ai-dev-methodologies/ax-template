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
    // L4 tests use @playwright/test and must run under `npx playwright test tests/L4/`
    // not under vitest. Excluding prevents the "test.describe() not expected here" error.
    exclude: ['tests/L4/**'],
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
  },
});
