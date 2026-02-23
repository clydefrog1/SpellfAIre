import { expect, test } from '@playwright/test';

test('mocked gameplay flow: setup, board, surrender, exit', async ({ page }) => {
  const mockUser = {
    id: '00000000-0000-0000-0000-000000000001',
    email: 'mock.player@example.com',
    username: 'MockMage',
    avatarBase64: null,
    rating: 1000,
  };

  const cards = [
    {
      id: 'card-creature-1',
      name: 'Mock Creature',
      cardType: 'CREATURE',
      cost: 1,
      attack: 1,
      health: 2,
      faction: 'KINGDOM',
      keywords: [],
      school: null,
      rulesText: null,
      flavorText: null,
    },
    {
      id: 'card-spell-1',
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
      deck: ['card-creature-1'],
      hand: ['card-spell-1'],
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
      deck: ['card-creature-1'],
      hand: [],
      battlefield: [],
      discardPile: [],
    },
    createdAt: '2026-02-23T00:00:00Z',
    updatedAt: '2026-02-23T00:00:00Z',
  };

  const finishedGame = {
    ...inProgressGame,
    gameStatus: 'FINISHED',
    winnerId: 'AI',
    updatedAt: '2026-02-23T00:00:01Z',
  };

  await page.route('**/api/auth/register', async route => {
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
  });

  await page.route('**/api/quotes/random', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ text: 'Mocked battle awaits.' }),
    });
  });

  await page.route('**/api/cards', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(cards),
    });
  });

  await page.route('**/api/games/ai', async route => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        game: inProgressGame,
        events: [],
      }),
    });
  });

  await page.route('**/api/games/*/surrender', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        game: finishedGame,
        events: [
          {
            type: 'GAME_OVER',
            sourceId: null,
            targetId: null,
            value: 0,
            message: 'Player surrendered',
          },
        ],
      }),
    });
  });

  await page.goto('/register');

  await page.getByLabel('Email').fill(mockUser.email);
  await page.getByLabel('Username').fill(mockUser.username);
  await page.getByLabel('Password').fill('password-123');
  await page.getByRole('button', { name: 'Create account' }).click();

  await page.getByRole('button', { name: 'Play vs AI' }).click();
  await expect(page).toHaveURL(/\/game\/setup$/);

  await page.getByRole('button', { name: 'Kingdom' }).click();
  await page.getByRole('button', { name: 'Fire' }).click();
  await page.getByRole('button', { name: 'Start Battle' }).click();

  await expect(page).toHaveURL(/\/game\/board$/);
  await expect(page.getByRole('button', { name: 'Surrender' })).toBeVisible();

  await page.getByRole('button', { name: 'Surrender' }).click();
  await expect(page.getByText('Defeat')).toBeVisible();

  await page.getByRole('button', { name: 'Exit' }).click();
  await expect(page).toHaveURL(/\/$/);
});
