import { expect, test } from '@playwright/test';

test('register, reach home, and logout against real backend', async ({ page }) => {
  const timestamp = Date.now();
  const email = `e2e.player.${timestamp}@example.com`;
  const username = `E2EPlayer${timestamp}`;

  await page.goto('/register');

  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill('password-123');
  await page.getByRole('button', { name: 'Create account' }).click();

  await expect(page.getByRole('heading', { name: 'SpellfAIre' })).toBeVisible();
  await expect(page.getByText(`Logged in as ${username} (${email})`)).toBeVisible();

  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page).toHaveURL(/\/login$/);
});
