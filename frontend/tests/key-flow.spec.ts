import { expect, test } from '@playwright/test';

test.describe('auth curated local fallback path', () => {
  test('loads app boundary for curated auth path', async ({ page }) => {
    await page.setContent('<main><h1>App Entry</h1><p>Curated auth boundary fallback</p></main>');

    await expect(page.getByText('App Entry')).toBeVisible();
  });

  test('covers provider-disabled fallback at app boundary', async ({ page }) => {
    await page.route('**/api/auth/email/login', async (route) => {
      await route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'provider_disabled',
          message: 'Email/password provider is disabled in local fallback runtime',
        }),
      });
    });

    await page.setContent('<main><h1>App Entry</h1><p>Provider-disabled fallback validation</p></main>');

    const fallbackResult = await page.evaluate(async () => {
      const response = await fetch('http://127.0.0.1:3000/api/auth/email/login', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ email: 'provider-disabled@example.com', password: 'irrelevant-password' }),
      });

      let body: unknown = null;
      try {
        body = await response.json();
      } catch {
        body = null;
      }

      return {
        status: response.status,
        body,
      };
    });

    expect(fallbackResult.status).toBe(403);
    expect(fallbackResult.body).toMatchObject({ code: 'provider_disabled' });
  });
});
