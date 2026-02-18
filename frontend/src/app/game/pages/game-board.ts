import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { GameService } from '../services/game.service';
import {
  BoardCreatureResponse,
  CardResponse,
  GameEvent,
} from '../models/game.models';
import { GameCard } from '../components/game-card';
import { BattlefieldCreature } from '../components/battlefield-creature';
import { HeroPortrait } from '../components/hero-portrait';
import { GameEventLog } from '../components/game-event-log';
import { GameOverOverlay } from '../components/game-over-overlay';
import {
  cardEnter,
  creatureSummon,
  turnPulse,
} from '../animations/game.animations';

@Component({
  selector: 'app-game-board',
  standalone: true,
  imports: [
    GameCard,
    BattlefieldCreature,
    HeroPortrait,
    GameEventLog,
    GameOverOverlay,
  ],
  templateUrl: './game-board.html',
  styleUrl: './game-board.scss',
  animations: [cardEnter, creatureSummon, turnPulse],
})
export class GameBoard implements OnInit {
  private readonly gameService = inject(GameService);
  private readonly router = inject(Router);

  readonly game = this.gameService.game;
  readonly myState = this.gameService.myState;
  readonly opponentState = this.gameService.opponentState;
  readonly isMyTurn = this.gameService.isMyTurn;
  readonly isGameOver = this.gameService.isGameOver;
  readonly didWin = this.gameService.didWin;
  readonly loading = this.gameService.loading;
  readonly events = this.gameService.events;

  // Selection state
  readonly selectedHandCardId = signal<string | null>(null);
  readonly selectedAttackerId = signal<string | null>(null);
  readonly selectedTargetId = signal<string | null>(null);
  readonly targetMode = signal<'none' | 'spell' | 'attack'>('none');
  readonly showTutorialHint = signal(true);
  
  // Ready to attack flag
  readonly readyToAttack = computed(() => {
    return this.selectedAttackerId() !== null && this.selectedTargetId() !== null;
  });

  // Computed hand cards — all cards, used internally
  readonly handCards = computed<CardResponse[]>(() => {
    const state = this.myState();
    if (!state) return [];
    return state.hand
      .map(id => this.gameService.getCard(id))
      .filter((c): c is CardResponse => c !== undefined);
  });

  /** Creature cards in hand, sorted by mana cost ascending. */
  readonly handCreatures = computed<CardResponse[]>(() =>
    this.handCards()
      .filter(c => c.cardType === 'CREATURE')
      .sort((a, b) => a.cost - b.cost)
  );

  /** Spell cards in hand, sorted by mana cost ascending. */
  readonly handSpells = computed<CardResponse[]>(() =>
    this.handCards()
      .filter(c => c.cardType === 'SPELL')
      .sort((a, b) => a.cost - b.cost)
  );

  // Check if card can be played
  canPlayCard(card: CardResponse): boolean {
    const state = this.myState();
    if (!state || !this.isMyTurn()) return false;
    if (card.cost > state.currentMana) return false;
    if (card.cardType === 'CREATURE' && state.battlefield.length >= 6) return false;
    return true;
  }

  /** True when the card costs more than the player's current mana. */
  isInsufficientMana(card: CardResponse): boolean {
    return (this.myState()?.currentMana ?? 0) < card.cost;
  }

  ngOnInit(): void {
    if (!this.game()) {
      this.router.navigateByUrl('/game/setup');
    }
    
    const seen = localStorage.getItem('sf-attack-tutorial-seen');
    this.showTutorialHint.set(!seen);
  }

  // ── Hand interaction ──

  onHandCardClick(card: CardResponse): void {
    if (!this.isMyTurn() || this.loading()) return;

    // If card needs targeting
    if (this.needsTarget(card)) {
      this.selectedHandCardId.set(card.id);
      this.targetMode.set('spell');
      this.selectedAttackerId.set(null);
      this.selectedTargetId.set(null);
      return;
    }

    // Play directly
    this.playCard(card.id);
  }

  private needsTarget(card: CardResponse): boolean {
    if (card.cardType !== 'SPELL') return false;
    const text = card.rulesText?.toLowerCase() ?? '';
    // Cards that need a target (creature or hero)
    return text.includes('target') || text.includes('a creature')
      || text.includes('an enemy creature') || text.includes('a friendly creature')
      || text.includes('enemy creature') || text.includes('friendly creature');
  }

  // ── Battlefield interaction ──

  onMyCreatureClick(creature: BoardCreatureResponse): void {
    if (!this.isMyTurn() || this.loading()) return;

    // If we're targeting for a spell on a friendly creature
    if (this.targetMode() === 'spell') {
      this.selectedTargetId.set(creature.instanceId);
      return;
    }

    // Select creature for attack
    if (creature.canAttack && !creature.hasAttackedThisTurn) {
      this.selectedAttackerId.set(creature.instanceId);
      this.selectedTargetId.set(null);
      this.targetMode.set('attack');
      this.selectedHandCardId.set(null);
    }
  }

  onEnemyCreatureClick(creature: BoardCreatureResponse): void {
    if (!this.isMyTurn() || this.loading()) return;

    if (this.targetMode() === 'spell') {
      this.selectedTargetId.set(creature.instanceId);
      return;
    }

    if (this.targetMode() === 'attack') {
      // Set the target, don't attack immediately
      this.selectedTargetId.set(creature.instanceId);
    }
  }

  onEnemyHeroClick(): void {
    if (!this.isMyTurn() || this.loading()) return;

    if (this.targetMode() === 'spell') {
      this.selectedTargetId.set('ENEMY_HERO');
      return;
    }

    if (this.targetMode() === 'attack') {
      // Set the target, don't attack immediately
      this.selectedTargetId.set('ENEMY_HERO');
    }
  }

  onMyHeroClick(): void {
    if (this.targetMode() === 'spell') {
      this.selectedTargetId.set('FRIENDLY_HERO');
    }
  }

  cancelSelection(): void {
    this.selectedHandCardId.set(null);
    this.selectedAttackerId.set(null);
    this.selectedTargetId.set(null);
    this.targetMode.set('none');
  }

  dismissHint(): void {
    this.showTutorialHint.set(false);
    localStorage.setItem('sf-attack-tutorial-seen', 'true');
  }
  
  async executeAttack(): Promise<void> {
    const attackerId = this.selectedAttackerId();
    const targetId = this.selectedTargetId();
    if (!attackerId || !targetId) return;
    
    await this.doAttack(attackerId, targetId);
  }

  async executeSpellCast(): Promise<void> {
    const cardId = this.selectedHandCardId();
    const targetId = this.selectedTargetId();
    if (!cardId || !targetId) return;

    await this.playCard(cardId, targetId);
  }

  // ── Actions ──

  private async playCard(cardId: string, targetId?: string): Promise<void> {
    this.cancelSelection();
    await this.gameService.playCard(cardId, targetId);
  }

  private async doAttack(attackerInstanceId: string, targetId: string): Promise<void> {
    this.cancelSelection();
    await this.gameService.attack(attackerInstanceId, targetId);
  }

  async endTurn(): Promise<void> {
    this.cancelSelection();
    await this.gameService.endTurn();
  }

  async surrender(): Promise<void> {
    await this.gameService.surrender();
  }

  exitGame(): void {
    this.gameService.reset();
    this.router.navigateByUrl('/');
  }

  getCard(id: string): CardResponse | undefined {
    return this.gameService.getCard(id);
  }

  trackByInstanceId(_: number, c: BoardCreatureResponse): string {
    return c.instanceId;
  }

  trackByCardId(_: number, c: CardResponse): string {
    return c.id;
  }
}
