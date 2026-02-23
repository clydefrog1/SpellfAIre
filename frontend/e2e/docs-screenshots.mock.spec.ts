import { expect, test } from '@playwright/test';
import path from 'node:path';

type CardResponse = {
  id: string;
  name: string;
  cardType: 'CREATURE' | 'SPELL';
  cost: number;
  attack: number | null;
  health: number | null;
  faction: 'KINGDOM' | 'WILDCLAN' | 'NECROPOLIS' | 'IRONBOUND' | null;
  keywords: readonly string[];
  school: 'FIRE' | 'FROST' | 'NATURE' | 'SHADOW' | null;
  rulesText: string | null;
  flavorText: string | null;
};

test.use({ viewport: { width: 1440, height: 900 } });

test('generate README screenshots (mocked API)', async ({ page }) => {
  const screenshotsDir = path.resolve(process.cwd(), '..', 'docs', 'screenshots');
  const screenshotPath = (name: string) => path.join(screenshotsDir, `${name}.png`);

  const mockUser = {
    id: '00000000-0000-0000-0000-000000000001',
    email: 'mock.player@example.com',
    username: 'MockMage',
    avatarBase64: null,
    rating: 1000,
  };

  const cards: readonly CardResponse[] = [
    // Game info example cards (names referenced by GameInfoPage)
    {
      id: 'card-kingdom-town-guard',
      name: 'Town Guard',
      cardType: 'CREATURE',
      cost: 1,
      attack: 1,
      health: 2,
      faction: 'KINGDOM',
      keywords: ['GUARD'],
      school: null,
      rulesText: null,
      flavorText: null,
    },
    {
      id: 'card-wildclan-razor-cub',
      name: 'Razor Cub',
      cardType: 'CREATURE',
      cost: 1,
      attack: 1,
      health: 1,
      faction: 'WILDCLAN',
      keywords: ['CHARGE'],
      school: null,
      rulesText: null,
      flavorText: null,
    },
    {
      id: 'card-ironbound-copper-drone',
      name: 'Copper Drone',
      cardType: 'CREATURE',
      cost: 1,
      attack: 1,
      health: 2,
      faction: 'IRONBOUND',
      keywords: ['WARD'],
      school: null,
      rulesText: null,
      flavorText: null,
    },
    {
      id: 'card-kingdom-high-paladin',
      name: 'High Paladin',
      cardType: 'CREATURE',
      cost: 7,
      attack: 6,
      health: 6,
      faction: 'KINGDOM',
      keywords: ['LIFESTEAL'],
      school: null,
      rulesText: null,
      flavorText: null,
    },
    {
      id: 'card-frost-ice-shard',
      name: 'Ice Shard',
      cardType: 'SPELL',
      cost: 1,
      attack: null,
      health: null,
      faction: null,
      keywords: [],
      school: 'FROST',
      rulesText: 'Deal 1 damage to a creature. Freeze it.',
      flavorText: null,
    },
    {
      id: 'card-fire-ember-bolt',
      name: 'Ember Bolt',
      cardType: 'SPELL',
      cost: 1,
      attack: null,
      health: null,
      faction: null,
      keywords: [],
      school: 'FIRE',
      rulesText: 'Deal 2 damage to any target.',
      flavorText: null,
    },

    // A couple extras so the library view looks less empty
    {
      id: 'card-necropolis-grave-rat',
      name: 'Grave Rat',
      cardType: 'CREATURE',
      cost: 1,
      attack: 1,
      health: 2,
      faction: 'NECROPOLIS',
      keywords: [],
      school: null,
      rulesText: 'When this dies, draw a card.',
      flavorText: null,
    },
    {
      id: 'card-shadow-dark-touch',
      name: 'Dark Touch',
      cardType: 'SPELL',
      cost: 1,
      attack: null,
      health: null,
      faction: null,
      keywords: [],
      school: 'SHADOW',
      rulesText: 'Deal 1 damage to any target. Heal your Hero for 1.',
      flavorText: null,
    },
  ];

  const inProgressGame = {
    id: '00000000-0000-0000-0000-000000000777',
    player1Id: mockUser.id,
    player2Id: 'AI',
    currentPlayerId: mockUser.id,
    gameStatus: 'IN_PROGRESS',
    currentPhase: 'MAIN',
    winnerId: null,
    turnNumber: 1,
    player1State: {
      userId: mockUser.id,
      deckId: 'deck-player',
      faction: 'KINGDOM',
      magicSchool: 'FIRE',
      heroHealth: 25,
      maxMana: 1,
      currentMana: 1,
      fatigueCounter: 0,
      deck: ['card-kingdom-town-guard'],
      hand: ['card-fire-ember-bolt'],
      battlefield: [],
      discardPile: [],
    },
    player2State: {
      userId: 'AI',
      deckId: 'deck-ai',
      faction: 'WILDCLAN',
      magicSchool: 'FROST',
      heroHealth: 25,
      maxMana: 1,
      currentMana: 1,
      fatigueCounter: 0,
      deck: ['card-wildclan-razor-cub'],
      hand: [],
      battlefield: [],
      discardPile: [],
    },
    createdAt: '2026-02-23T00:00:00Z',
    updatedAt: '2026-02-23T00:00:00Z',
  };

  await page.addStyleTag({
    content: `
      *, *::before, *::after {
        transition-duration: 0s !important;
        animation-duration: 0s !important;
        animation-delay: 0s !important;
        caret-color: transparent !important;
      }
    `,
  });

  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url());
    const pathName = url.pathname;
    const method = route.request().method();

    if (pathName.endsWith('/api/auth/me') && method === 'GET') {
      await route.fulfill({ status: 401 });
      return;
    }

    if (pathName.endsWith('/api/auth/refresh') && method === 'POST') {
      await route.fulfill({ status: 401 });
      return;
    }

    if (pathName.endsWith('/api/auth/register') && method === 'POST') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: {
          'Set-Cookie': 'spellfaire_refresh=mock-token; Path=/api/auth; HttpOnly; SameSite=Lax',
        },
        body: JSON.stringify({
          accessToken: 'mock-access-token',
          user: mockUser,
        }),
      });
      return;
    }

    if (pathName.endsWith('/api/auth/logout') && method === 'POST') {
      await route.fulfill({ status: 204 });
      return;
    }

    if (pathName.endsWith('/api/quotes/random') && method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ text: 'The arcane winds answer bold hearts.' }),
      });
      return;
    }

    if (pathName.endsWith('/api/cards') && method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(cards),
      });
      return;
    }

    if (pathName.endsWith('/api/games/ai') && method === 'POST') {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({ game: inProgressGame, events: [] }),
      });
      return;
    }

    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ message: `No mock for ${method} ${pathName}` }),
    });
  });

  // 1) Login
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
  await page.screenshot({ path: screenshotPath('login'), fullPage: true });

  // 2) Register (empty)
  await page.goto('/register');
  await expect(page.getByRole('heading', { name: 'Register' })).toBeVisible();
  await page.screenshot({ path: screenshotPath('register'), fullPage: true });

  // Register to land on Home.
  await page.getByLabel('Email').fill(mockUser.email);
  await page.getByLabel('Username').fill(mockUser.username);
  await page.getByLabel('Password').fill('password-123');
  await page.getByRole('button', { name: 'Create account' }).click();

  // 3) Home
  await expect(page.getByRole('heading', { name: 'SpellfAIre' })).toBeVisible();
  await expect(page.getByText(`Welcome, ${mockUser.username}`)).toBeVisible();
  await page.screenshot({ path: screenshotPath('home'), fullPage: true });

  // 4) Game Info
  await page.getByRole('button', { name: 'Game Info' }).click();
  await expect(page.getByRole('heading', { name: 'Game Info' })).toBeVisible();
  await page.screenshot({ path: screenshotPath('game-info'), fullPage: true });

  // 5) Card Library
  await page.getByRole('button', { name: '← Back' }).click();
  await expect(page.getByRole('heading', { name: 'SpellfAIre' })).toBeVisible();

  await page.getByRole('button', { name: 'Card Library' }).click();
  await expect(page.getByRole('heading', { name: 'Card Library' })).toBeVisible();
  await page.screenshot({ path: screenshotPath('card-library'), fullPage: true });

  // 6) Game Setup
  await page.getByRole('button', { name: '← Back' }).click();
  await expect(page.getByRole('heading', { name: 'SpellfAIre' })).toBeVisible();

  await page.getByRole('button', { name: 'Play vs AI' }).click();
  await expect(page.getByRole('heading', { name: 'New Game' })).toBeVisible();
  await page.screenshot({ path: screenshotPath('game-setup'), fullPage: true });

  // 7) Game Board
  await page.getByRole('button', { name: 'Kingdom' }).click();
  await page.getByRole('button', { name: 'Fire' }).click();
  await page.getByRole('button', { name: 'Start Battle' }).click();

  await expect(page).toHaveURL(/\/game\/board$/);
  await expect(page.getByRole('button', { name: 'Surrender' })).toBeVisible();
  await page.screenshot({ path: screenshotPath('game-board'), fullPage: true });
});
