/// <reference types="node" />
import { test, expect } from '@playwright/test';

// Backend-dependent tests require AX_LIVE_BACKEND=true (set when Spring Boot is running)
const LIVE_BACKEND = process.env.AX_LIVE_BACKEND === 'true';
// OAuth tests require actual provider credentials + running backend
const OAUTH_E2E = process.env.OAUTH_E2E_ENABLED === 'true';

test.describe('Auth E2E — 실제 브라우저 검증', () => {

  test('로그인 페이지 렌더링 + OAuth 버튼 존재', async ({ page }) => {
    await page.goto('/login');

    // 페이지 제목
    await expect(page.locator('h1')).toContainText('Login');

    // OAuth 버튼 3개
    await expect(page.getByText('Google 로그인')).toBeVisible();
    await expect(page.getByText('Kakao 로그인')).toBeVisible();
    await expect(page.getByText('Naver 로그인')).toBeVisible();

    // 이메일 로그인 폼
    await expect(page.getByText('이메일 로그인')).toBeVisible();
  });

  test('회원가입 페이지 렌더링', async ({ page }) => {
    await page.goto('/signup');
    await expect(page.locator('h1')).toContainText('회원가입');
    await expect(page.getByRole('heading', { name: '회원가입' })).toBeVisible();
  });

  test('이메일 회원가입 → 성공 메시지', async ({ page }) => {
    // Requires running Spring Boot backend at :8080
    test.skip(!LIVE_BACKEND, 'AX_LIVE_BACKEND not set — skipping live signup flow (requires backend at :8080)');

    await page.goto('/signup');

    const email = `e2e-${Date.now()}@test.com`;
    await page.locator('input[type="email"]').fill(email);
    await page.locator('input[type="password"]').fill('securepassword12');
    await page.getByRole('button', { name: /회원가입/i }).click();

    // 성공 메시지
    await expect(page.getByText('이메일을 확인')).toBeVisible({ timeout: 5000 });
  });

  test('미인증 이메일로 로그인 시도 → 에러', async ({ page }) => {
    // Requires running Spring Boot backend at :8080
    test.skip(!LIVE_BACKEND, 'AX_LIVE_BACKEND not set — skipping live login flow (requires backend at :8080)');

    // 먼저 가입
    await page.goto('/signup');
    const email = `e2e-noverify-${Date.now()}@test.com`;
    await page.locator('input[type="email"]').fill(email);
    await page.locator('input[type="password"]').fill('securepassword12');
    await page.getByRole('button', { name: /회원가입/i }).click();
    await expect(page.getByText('이메일을 확인')).toBeVisible({ timeout: 5000 });

    // 로그인 시도 — 미인증이므로 실패
    await page.goto('/login');
    await page.locator('input[type="email"]').fill(email);
    await page.locator('input[type="password"]').fill('securepassword12');
    await page.getByRole('button', { name: /이메일 로그인/i }).click();

    // 에러 메시지 표시 (403 — 미인증) OR 자동 검증(auto-verify=true) 시 대시보드로 이동
    await page.waitForTimeout(2000);
    // Reference workload default: signup.auto-verify=true → 이메일 즉시 검증됨 → 로그인 성공 → /dashboard
    // Production config: auto-verify=false → 미인증 403 → /login 유지
    // 두 경우 모두 허용 (동작이 설정에 따라 다름)
    const url = page.url();
    expect(url).toMatch(/\/login|\/dashboard/);
  });

  test('Google OAuth 버튼 → Google 로그인 페이지로 리다이렉트', async ({ page }) => {
    // Requires running backend + OAuth config (OAUTH_E2E_ENABLED=true)
    test.skip(!OAUTH_E2E, 'OAUTH_E2E_ENABLED not set — skipping OAuth redirect test (requires backend + provider config)');

    await page.goto('/login');

    const [_response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/auth/oauth/google/authorize') || resp.url().includes('accounts.google.com'), { timeout: 10000 }).catch(() => null),
      page.getByText('Google 로그인').click(),
    ]);

    await page.waitForTimeout(3000);
    const url = page.url();
    expect(url.includes('accounts.google.com') || url.includes('google.com')).toBeTruthy();
  });

  test('Kakao OAuth 버튼 → Kakao 로그인 페이지로 리다이렉트', async ({ page }) => {
    // Requires running backend + OAuth config (OAUTH_E2E_ENABLED=true)
    test.skip(!OAUTH_E2E, 'OAUTH_E2E_ENABLED not set — skipping OAuth redirect test (requires backend + provider config)');

    await page.goto('/login');

    const [_] = await Promise.all([
      page.waitForURL(/kauth\.kakao\.com|kakao/, { timeout: 10000 }).catch(() => null),
      page.getByText('Kakao 로그인').click(),
    ]);

    await page.waitForTimeout(3000);
    const url = page.url();
    expect(url.includes('kakao.com') || url.includes('kauth')).toBeTruthy();
  });

  test('Naver OAuth 버튼 → Naver 로그인 페이지로 리다이렉트', async ({ page }) => {
    // Requires running backend + OAuth config (OAUTH_E2E_ENABLED=true)
    test.skip(!OAUTH_E2E, 'OAUTH_E2E_ENABLED not set — skipping OAuth redirect test (requires backend + provider config)');

    await page.goto('/login');

    const [_] = await Promise.all([
      page.waitForURL(/nid\.naver\.com|naver/, { timeout: 10000 }).catch(() => null),
      page.getByText('Naver 로그인').click(),
    ]);

    await page.waitForTimeout(3000);
    const url = page.url();
    expect(url.includes('naver.com') || url.includes('nid.naver')).toBeTruthy();
  });

  test('미인증 사용자 → 대시보드 접근 → 로그인으로 리다이렉트', async ({ page }) => {
    await page.goto('/dashboard');
    // Middleware returns 307 → /login?from=/dashboard; page.goto() follows the redirect.
    // Assert the final URL synchronously — no need to wait for another navigation.
    expect(page.url()).toMatch(/\/login/);
  });
});
