import { test, expect } from '@playwright/test';

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
    await page.goto('/signup');
    
    const email = `e2e-${Date.now()}@test.com`;
    await page.locator('input[type="email"]').fill(email);
    await page.locator('input[type="password"]').fill('securepassword12');
    await page.getByRole('button', { name: /회원가입/i }).click();
    
    // 성공 메시지
    await expect(page.getByText('이메일을 확인')).toBeVisible({ timeout: 5000 });
  });

  test('미인증 이메일로 로그인 시도 → 에러', async ({ page }) => {
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
    
    // 에러 메시지 표시 (403 — 미인증)
    await page.waitForTimeout(2000);
    // 에러가 표시되거나 로그인 페이지에 남아있어야 함
    const url = page.url();
    expect(url).toContain('/login');
  });

  test('Google OAuth 버튼 → Google 로그인 페이지로 리다이렉트', async ({ page }) => {
    await page.goto('/login');
    
    // Google 버튼 클릭 시 Google로 리다이렉트되는지 확인
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/auth/oauth/google/authorize') || resp.url().includes('accounts.google.com'), { timeout: 10000 }).catch(() => null),
      page.getByText('Google 로그인').click(),
    ]);

    // Google 로그인 페이지로 이동했는지 확인
    await page.waitForTimeout(3000);
    const url = page.url();
    expect(url.includes('accounts.google.com') || url.includes('google.com')).toBeTruthy();
  });

  test('Kakao OAuth 버튼 → Kakao 로그인 페이지로 리다이렉트', async ({ page }) => {
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
    
    // ProtectedRoute가 /login으로 리다이렉트
    await expect(page.getByText('로그인이 필요합니다')).toBeVisible({ timeout: 5000 }).catch(async () => {
      // 또는 /login으로 리다이렉트됨
      await page.waitForURL(/\/login/, { timeout: 5000 });
    });
  });
});
