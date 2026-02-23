import { expect, test } from '@playwright/test';

test('real backend gameplay smoke: setup and surrender', async ({ page }) => {
  const timestamp = Date.now();
  const email = `e2e.gameplay.${timestamp}@example.com`;
  const username = `E2EGame${timestamp}`;

  await page.goto('/register');

  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill('password-123');
  await page.getByRole('button', { name: 'Create account' }).click();

  await expect(page.getByRole('heading', { name: 'SpellfAIre' })).toBeVisible();

  await page.getByRole('button', { name: 'Play vs AI' }).click();
  await expect(page).toHaveURL(/\/game\/setup$/);

  await page.getByRole('button', { name: 'Kingdom' }).click();
  await page.getByRole('button', { name: 'Fire' }).click();
  await page.getByRole('button', { name: 'Start Battle' }).click();

  await expect(page).toHaveURL(/\/game\/board$/);
  await expect(page.getByRole('button', { name: 'Surrender' })).toBeVisible();

  await page.getByRole('button', { name: 'Surrender' }).click();
  await expect(page.getByText('Defeat')).toBeVisible();
});
