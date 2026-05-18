import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  // L1/L2/L3 *.spec.ts files import from 'vitest' and run under Vitest, not Playwright.
  // Only match: L4 composition specs, top-level e2e-*.spec.ts, and auth/login-flow.spec.ts.
  testMatch: [
    'L4/**/*.spec.ts',
    'e2e-*.spec.ts',
    'key-flow.spec.ts',
    'auth/**/*.spec.ts',
    'recipes/**/*.spec.ts',
  ],
  fullyParallel: false,
  retries: 0,
  timeout: 30000,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'off',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run build && npm run start',
    port: 3000,
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
});
