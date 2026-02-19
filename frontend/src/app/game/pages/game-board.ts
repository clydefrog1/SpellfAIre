import { Component, inject, signal, computed, effect, OnInit } from '@angular/core';
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

  private lastProcessedEventIndex = 0;
  private fxToken = 0;
  private currentActorUserId: string | null = null;

  readonly game = this.gameService.game;
  readonly myState = this.gameService.myState;
  readonly opponentState = this.gameService.opponentState;
  readonly isMyTurn = this.gameService.isMyTurn;
  readonly isGameOver = this.gameService.isGameOver;
  readonly didWin = this.gameService.didWin;
  readonly loading = this.gameService.loading;
  readonly events = this.gameService.events;

  readonly myUserId = computed(() => this.myState()?.userId ?? null);
  readonly opponentUserId = computed(() => this.opponentState()?.userId ?? null);

  // ── Combat FX (event-driven, transient UI state) ──

  readonly attackAnim = signal<
    { attackerId: string; targetId: string; token: number } | null
  >(null);

  readonly hitFlashByTarget = signal<
    Record<string, { kind: 'damage' | 'heal'; token: number }>
  >({});

  readonly floatFxByTarget = signal<
    Record<string, { kind: 'damage' | 'heal'; value: number; token: number }>
  >({});

  readonly spellImpactByTarget = signal<
    Record<string, { kind: 'benefit' | 'harm'; token: number }>
  >({});

  // Selection state
  readonly selectedHandCardId = signal<string | null>(null);
  readonly selectedAttackerId = signal<string | null>(null);
  readonly selectedTargetId = signal<string | null>(null);
  readonly targetMode = signal<'none' | 'spell' | 'attack'>('none');
  readonly showTutorialHint = signal(true);

  // Guard restriction (UI enforcement)
  readonly opponentGuardCreatures = computed(() => {
    const battlefield = this.opponentState()?.battlefield ?? [];
    return battlefield.filter(c => c.keywords?.includes('GUARD'));
  });

  readonly disableEnemyHeroAttackTarget = computed(() => {
    const isAttackTargeting =
      this.targetMode() === 'attack' && this.selectedAttackerId() !== null;
    return isAttackTargeting && this.opponentGuardCreatures().length > 0;
  });

  readonly enemyHeroGuardMessage = computed<string | null>(() => {
    if (!this.disableEnemyHeroAttackTarget()) return null;

    const guards = this.opponentGuardCreatures();
    if (guards.length === 0) return null;

    const guardNames = guards
      .map(c => this.getCard(c.cardId)?.name)
      .filter((n): n is string => !!n);

    const suffix = guardNames.length > 0 ? guardNames.join(', ') : 'Guard creatures';
    return `Guarded by: ${suffix}`;
  });

  readonly enemyHeroGuardHintToken = signal(0);
  
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

  readonly selectedSpellCard = computed<CardResponse | null>(() => {
    if (this.targetMode() !== 'spell') return null;
    const cardId = this.selectedHandCardId();
    if (!cardId) return null;
    const card = this.getCard(cardId);
    return card?.cardType === 'SPELL' ? card : null;
  });

  /** Prevent self-targeting when selecting a damage spell. */
  readonly spellDisallowsFriendlyTargets = computed(() => {
    const card = this.selectedSpellCard();
    if (!card) return false;
    return this.isDamageSpell(card);
  });

  private readonly combatFxEffect = effect(() => {
    const allEvents = this.events();

    if (allEvents.length < this.lastProcessedEventIndex) {
      this.lastProcessedEventIndex = 0;
    }

    const newEvents = allEvents.slice(this.lastProcessedEventIndex);
    this.lastProcessedEventIndex = allEvents.length;

    let delayMs = 0;

    let inSpellResolution = false;
    let spellCastKind: 'benefit' | 'harm' = 'benefit';

    for (const event of newEvents) {
      if (event.type === 'TURN_START' && event.sourceId) {
        this.currentActorUserId = event.sourceId;
      }

      if (event.type === 'CARD_PLAYED' && event.sourceId) {
        const card = this.getCard(event.sourceId);
        inSpellResolution = card?.cardType === 'SPELL';

        if (inSpellResolution && card) {
          spellCastKind = this.classifySpellCastKind(card);

          const casterId = this.currentActorUserId ?? this.myUserId() ?? this.opponentUserId();
          if (casterId) {
            this.scheduleSpellImpactFx(casterId, spellCastKind, delayMs);
          }
        }
      }

      this.scheduleCombatFxForEvent(event, delayMs);

      if (inSpellResolution) {
        this.scheduleSpellImpactFromResolutionEvent(event, delayMs, spellCastKind);
      }

      const shouldStagger =
        event.type === 'ATTACK' ||
        event.type === 'DAMAGE' ||
        event.type === 'HEAL' ||
        (inSpellResolution &&
          (event.type === 'BUFF' ||
            event.type === 'FREEZE' ||
            event.type === 'DEATH' ||
            event.type === 'SUMMON'));

      if (shouldStagger) delayMs += 280;
    }
  });

  private classifySpellCastKind(card: CardResponse): 'benefit' | 'harm' {
    const text = card.rulesText?.toLowerCase() ?? '';

    if (text.includes('damage') || text.includes('destroy') || text.includes('freeze')) {
      return 'harm';
    }

    if (text.includes('heal') || text.includes('summon') || text.includes('give')) {
      return 'benefit';
    }

    return 'benefit';
  }

  private scheduleSpellImpactFromResolutionEvent(
    event: GameEvent,
    delayMs: number,
    defaultKind: 'benefit' | 'harm'
  ): void {
    if (event.type === 'DAMAGE' && event.targetId) {
      this.scheduleSpellImpactFx(event.targetId, 'harm', delayMs);
      return;
    }

    if (event.type === 'HEAL' && event.targetId) {
      this.scheduleSpellImpactFx(event.targetId, 'benefit', delayMs);
      return;
    }

    if (event.type === 'FREEZE' && event.targetId) {
      this.scheduleSpellImpactFx(event.targetId, 'harm', delayMs);
      return;
    }

    if (event.type === 'BUFF' && event.targetId) {
      const kind: 'benefit' | 'harm' = event.value < 0 ? 'harm' : 'benefit';
      this.scheduleSpellImpactFx(event.targetId, kind, delayMs);
      return;
    }

    if (event.type === 'DEATH' && event.sourceId) {
      this.scheduleSpellImpactFx(event.sourceId, 'harm', delayMs);
      return;
    }

    if (event.type === 'SUMMON' && event.sourceId) {
      // Some token summons emit a non-unique placeholder id ('token'); avoid pulsing the wrong creature.
      if (event.sourceId !== 'token') {
        this.scheduleSpellImpactFx(event.sourceId, defaultKind, delayMs);
      }
    }
  }

  private scheduleSpellImpactFx(
    targetId: string,
    kind: 'benefit' | 'harm',
    delayMs: number
  ): void {
    const token = ++this.fxToken;
    const durationMs = 1560;

    window.setTimeout(() => {
      this.spellImpactByTarget.update(map => ({
        ...map,
        [targetId]: { kind, token },
      }));

      window.setTimeout(() => {
        this.spellImpactByTarget.update(map => {
          const current = map[targetId];
          if (!current || current.token !== token) return map;
          const { [targetId]: _, ...rest } = map;
          return rest;
        });
      }, durationMs);
    }, delayMs);
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
      if (this.spellDisallowsFriendlyTargets()) return;
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
      if (this.disableEnemyHeroAttackTarget()) return;
      // Set the target, don't attack immediately
      this.selectedTargetId.set('ENEMY_HERO');
    }
  }

  onEnemyHeroDisabledClick(): void {
    if (!this.disableEnemyHeroAttackTarget()) return;
    this.enemyHeroGuardHintToken.update(v => v + 1);
  }

  onMyHeroClick(): void {
    if (this.targetMode() === 'spell') {
      if (this.spellDisallowsFriendlyTargets()) return;
      this.selectedTargetId.set('FRIENDLY_HERO');
    }
  }

  private isDamageSpell(card: CardResponse): boolean {
    if (card.cardType !== 'SPELL') return false;
    const text = card.rulesText?.toLowerCase() ?? '';
    return text.includes('damage') && (text.includes('deal') || text.includes('takes'));
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

  private scheduleCombatFxForEvent(event: GameEvent, delayMs: number): void {
    if (event.type === 'ATTACK') {
      if (!event.sourceId || !event.targetId) return;

      const resolvedTargetId = this.resolveAttackTargetId(event.sourceId, event.targetId);
      if (!resolvedTargetId) return;

      const token = ++this.fxToken;
      window.setTimeout(() => {
        this.attackAnim.set({ attackerId: event.sourceId!, targetId: resolvedTargetId, token });

        window.setTimeout(() => {
          const current = this.attackAnim();
          if (current?.token === token) {
            this.attackAnim.set(null);
          }
        }, 640);
      }, delayMs);
      return;
    }

    if (event.type === 'DAMAGE' || event.type === 'HEAL') {
      if (!event.targetId) return;

      const value = Math.abs(event.value ?? 0);
      if (value <= 0) return;

      const kind: 'damage' | 'heal' = event.type === 'DAMAGE' ? 'damage' : 'heal';
      const token = ++this.fxToken;
      const targetId = event.targetId;

      window.setTimeout(() => {
        this.hitFlashByTarget.update(map => ({
          ...map,
          [targetId]: { kind, token },
        }));

        this.floatFxByTarget.update(map => ({
          ...map,
          [targetId]: { kind, value, token },
        }));

        window.setTimeout(() => {
          this.hitFlashByTarget.update(map => {
            const current = map[targetId];
            if (!current || current.token !== token) return map;
            const { [targetId]: _, ...rest } = map;
            return rest;
          });
        }, 900);

        window.setTimeout(() => {
          this.floatFxByTarget.update(map => {
            const current = map[targetId];
            if (!current || current.token !== token) return map;
            const { [targetId]: _, ...rest } = map;
            return rest;
          });
        }, 1800);
      }, delayMs);
    }
  }

  private resolveAttackTargetId(attackerInstanceId: string, targetId: string): string | null {
    if (targetId !== 'ENEMY_HERO' && targetId !== 'FRIENDLY_HERO') {
      return targetId;
    }

    const attackerSide = this.getCreatureSide(attackerInstanceId);
    if (!attackerSide) return null;

    const myId = this.myUserId();
    const oppId = this.opponentUserId();
    if (!myId || !oppId) return null;

    if (targetId === 'ENEMY_HERO') {
      return attackerSide === 'player' ? oppId : myId;
    }

    return attackerSide === 'player' ? myId : oppId;
  }

  private getCreatureSide(instanceId: string): 'player' | 'opponent' | null {
    const myBattlefield = this.myState()?.battlefield ?? [];
    if (myBattlefield.some(c => c.instanceId === instanceId)) return 'player';

    const opponentBattlefield = this.opponentState()?.battlefield ?? [];
    if (opponentBattlefield.some(c => c.instanceId === instanceId)) return 'opponent';

    return null;
  }

  getAttackSourceToken(id: string): number | null {
    const anim = this.attackAnim();
    return anim?.attackerId === id ? anim.token : null;
  }

  getAttackTargetToken(id: string): number | null {
    const anim = this.attackAnim();
    return anim?.targetId === id ? anim.token : null;
  }

  getHitFx(id: string): { kind: 'damage' | 'heal'; token: number } | null {
    return this.hitFlashByTarget()[id] ?? null;
  }

  getFloatFx(id: string): { kind: 'damage' | 'heal'; value: number; token: number } | null {
    return this.floatFxByTarget()[id] ?? null;
  }

  getSpellFx(id: string): { kind: 'benefit' | 'harm'; token: number } | null {
    return this.spellImpactByTarget()[id] ?? null;
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
