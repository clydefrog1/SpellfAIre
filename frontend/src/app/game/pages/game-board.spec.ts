import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { GameBoard } from './game-board';
import {
  BoardCreatureResponse,
  CardResponse,
  GameEvent,
  GamePlayerStateResponse,
  GameResponse,
} from '../models/game.models';
import { GameService } from '../services/game.service';

class MockGameService {
  private readonly _game = signal<GameResponse | null>(null);
  private readonly _events = signal<GameEvent[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly cardMap = new Map<string, CardResponse>();

  readonly game = this._game.asReadonly();
  readonly events = this._events.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly isMyTurn = computed(() => {
    const g = this._game();
    if (!g) return false;
    return g.currentPlayerId === g.player1Id && g.gameStatus === 'IN_PROGRESS';
  });

  readonly myState = computed(() => this._game()?.player1State ?? null);
  readonly opponentState = computed(() => this._game()?.player2State ?? null);
  readonly isGameOver = computed(() => this._game()?.gameStatus === 'FINISHED');
  readonly didWin = computed(() => null);

  setGame(game: GameResponse): void {
    this._game.set(game);
  }

  setCards(cards: CardResponse[]): void {
    this.cardMap.clear();
    for (const card of cards) {
      this.cardMap.set(card.id, card);
    }
  }

  getCard(id: string): CardResponse | undefined {
    return this.cardMap.get(id);
  }

  async playCard(): Promise<boolean> {
    return true;
  }

  async attack(): Promise<boolean> {
    return true;
  }

  async endTurn(): Promise<boolean> {
    return true;
  }

  async surrender(): Promise<boolean> {
    return true;
  }

  reset(): void {
    this._game.set(null);
    this._events.set([]);
  }
}

describe('GameBoard Grim Bargain targeting', () => {
  const grimBargain: CardResponse = {
    id: 'grim-bargain',
    name: 'Grim Bargain',
    cardType: 'SPELL',
    cost: 4,
    attack: null,
    health: null,
    faction: null,
    keywords: [],
    school: 'SHADOW',
    rulesText: 'Destroy one of your creatures. Draw 2 cards.',
    flavorText: null,
  };

  const emberBolt: CardResponse = {
    id: 'ember-bolt',
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
  };

  const myCreature = (instanceId: string): BoardCreatureResponse => ({
    instanceId,
    cardId: 'friendly-card',
    attack: 2,
    health: 2,
    maxHealth: 2,
    canAttack: true,
    hasAttackedThisTurn: false,
    frozenBlocksAttacksThisTurn: false,
    keywords: [],
    statuses: [],
    position: 0,
  });

  const enemyCreature = (instanceId: string): BoardCreatureResponse => ({
    instanceId,
    cardId: 'enemy-card',
    attack: 2,
    health: 2,
    maxHealth: 2,
    canAttack: false,
    hasAttackedThisTurn: false,
    frozenBlocksAttacksThisTurn: false,
    keywords: [],
    statuses: [],
    position: 0,
  });

  const makePlayerState = (userId: string, battlefield: BoardCreatureResponse[], hand: string[]): GamePlayerStateResponse => ({
    userId,
    deckId: `${userId}-deck`,
    faction: userId === 'p1' ? 'NECROPOLIS' : 'WILDCLAN',
    magicSchool: userId === 'p1' ? 'SHADOW' : 'FROST',
    heroHealth: 25,
    maxMana: 5,
    currentMana: 5,
    fatigueCounter: 0,
    deck: [],
    hand,
    battlefield,
    discardPile: [],
  });

  const makeGame = (): GameResponse => ({
    id: 'game-1',
    player1Id: 'p1',
    player2Id: 'p2',
    currentPlayerId: 'p1',
    gameStatus: 'IN_PROGRESS',
    currentPhase: 'MAIN',
    winnerId: null,
    turnNumber: 3,
    player1State: makePlayerState('p1', [myCreature('friendly-1')], ['grim-bargain', 'ember-bolt']),
    player2State: makePlayerState('p2', [enemyCreature('enemy-1')], []),
    createdAt: '2026-02-23T00:00:00Z',
    updatedAt: '2026-02-23T00:00:00Z',
  });

  let gameService: MockGameService;

  beforeEach(async () => {
    gameService = new MockGameService();
    gameService.setCards([grimBargain, emberBolt]);
    gameService.setGame(makeGame());

    await TestBed.configureTestingModule({
      imports: [GameBoard],
      providers: [
        { provide: GameService, useValue: gameService },
        {
          provide: Router,
          useValue: {
            navigateByUrl: jasmine.createSpy('navigateByUrl'),
          },
        },
      ],
    }).compileComponents();
  });

  it('enters spell targeting mode when Grim Bargain is clicked', () => {
    const fixture = TestBed.createComponent(GameBoard);
    fixture.detectChanges();

    fixture.componentInstance.onHandCardClick(grimBargain);

    expect(fixture.componentInstance.targetMode()).toBe('spell');
    expect(fixture.componentInstance.selectedHandCardId()).toBe('grim-bargain');
    expect(fixture.componentInstance.spellRequiresFriendlyCreatureTarget()).toBeTrue();
  });

  it('does not allow selecting enemy targets for Grim Bargain', () => {
    const fixture = TestBed.createComponent(GameBoard);
    fixture.detectChanges();

    fixture.componentInstance.onHandCardClick(grimBargain);
    fixture.componentInstance.onEnemyCreatureClick(enemyCreature('enemy-1'));
    fixture.componentInstance.onEnemyHeroClick();

    expect(fixture.componentInstance.selectedTargetId()).toBeNull();
  });

  it('allows selecting a friendly creature for Grim Bargain', () => {
    const fixture = TestBed.createComponent(GameBoard);
    fixture.detectChanges();

    fixture.componentInstance.onHandCardClick(grimBargain);
    fixture.componentInstance.onMyCreatureClick(myCreature('friendly-1'));

    expect(fixture.componentInstance.selectedTargetId()).toBe('friendly-1');
  });
});
