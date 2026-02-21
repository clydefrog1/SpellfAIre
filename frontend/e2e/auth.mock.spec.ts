import { expect, test } from '@playwright/test';

test('register and logout with mocked API endpoints', async ({ page }) => {
  const mockUser = {
    id: '00000000-0000-0000-0000-000000000001',
    email: 'mock.player@example.com',
    username: 'MockMage',
    avatarBase64: null,
    rating: 1000
  };

  await page.route('**/api/auth/register', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      headers: {
        'Set-Cookie': 'spellfaire_refresh=mock-token; Path=/api/auth; HttpOnly; SameSite=Lax'
      },
      body: JSON.stringify({
        accessToken: 'mock-access-token',
        user: mockUser
      })
    });
  });

  await page.route('**/api/auth/logout', async route => {
    await route.fulfill({ status: 204 });
  });

  await page.route('**/api/quotes/random', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ text: 'The arcane winds answer bold hearts.' })
    });
  });

  await page.goto('/register');

  await page.getByLabel('Email').fill(mockUser.email);
  await page.getByLabel('Username').fill(mockUser.username);
  await page.getByLabel('Password').fill('password-123');
  await page.getByRole('button', { name: 'Create account' }).click();

  await expect(page.getByRole('heading', { name: 'SpellfAIre' })).toBeVisible();
  await expect(page.getByText('Welcome, MockMage')).toBeVisible();

  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page).toHaveURL(/\/login$/);
});
