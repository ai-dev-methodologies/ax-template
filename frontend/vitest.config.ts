import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    // Array form so the @ax/blocks subpath alias (regex) can sit alongside the
    // bare-specifier aliases. Order matters: more specific patterns first.
    alias: [
      { find: /^@ax\/ui$/, replacement: path.resolve(__dirname, './packages/ui/src/index.ts') },
      { find: /^@ax\/core$/, replacement: path.resolve(__dirname, './packages/core/src/index.ts') },
      { find: /^@ax\/blocks$/, replacement: path.resolve(__dirname, './packages/blocks/src/index.ts') },
      { find: /^@ax\/blocks\/(.*)$/, replacement: path.resolve(__dirname, './packages/blocks/src') + '/$1' },
      { find: /^@\/(.*)$/, replacement: path.resolve(__dirname, './src') + '/$1' },
      // Kit unit tests import repo-root templates/* hook files that statically
      // import next/navigation; vite can't resolve that bare specifier for a
      // file outside the project root. Point it at an inert stub. Component
      // tests that exercise the hooks vi.mock('next/navigation') over this.
      { find: /^next\/navigation$/, replacement: path.resolve(__dirname, './tests/_stubs/next-navigation.ts') },
      // Shipped catalog blocks import the L0 kit by the repo-root specifier
      // `templates/L0/...` (see templates/L2/blocks/crud-*-form.tsx and
      // payment-checkout-form.tsx) — the path a fork-receiver aliases in its own
      // tsconfig. Map it here so a test that renders such a block can resolve it.
      { find: /^templates\/(.*)$/, replacement: path.resolve(__dirname, '../templates') + '/$1' },
    ],
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
