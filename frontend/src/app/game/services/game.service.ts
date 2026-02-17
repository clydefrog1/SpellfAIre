import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../../api/api-base-url.token';
import {
  AttackRequest,
  CardResponse,
  CreateAiGameRequest,
  GameActionResponse,
  GameEvent,
  GameResponse,
  GamePlayerStateResponse,
  PlayCardRequest,
} from '../models/game.models';

@Injectable({ providedIn: 'root' })
export class GameService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  // ── Card cache ──────────────────────────────────────
  private readonly _cards = signal<Map<string, CardResponse>>(new Map());
  readonly cards = this._cards.asReadonly();

  // ── Active game state ───────────────────────────────
  private readonly _game = signal<GameResponse | null>(null);
  readonly game = this._game.asReadonly();

  private readonly _events = signal<GameEvent[]>([]);
  readonly events = this._events.asReadonly();

  private readonly _loading = signal(false);
  readonly loading = this._loading.asReadonly();

  private readonly _error = signal<string | null>(null);
  readonly error = this._error.asReadonly();

  // ── Derived state ───────────────────────────────────
  readonly isMyTurn = computed(() => {
    const g = this._game();
    if (!g) return false;
    return g.currentPlayerId === g.player1Id && g.gameStatus === 'IN_PROGRESS';
  });

  readonly myState = computed<GamePlayerStateResponse | null>(() => {
    const g = this._game();
    return g?.player1State ?? null;
  });

  readonly opponentState = computed<GamePlayerStateResponse | null>(() => {
    const g = this._game();
    return g?.player2State ?? null;
  });

  readonly isGameOver = computed(() => {
    const g = this._game();
    return g?.gameStatus === 'FINISHED';
  });

  readonly didWin = computed(() => {
    const g = this._game();
    if (!g || g.gameStatus !== 'FINISHED') return null;
    return g.winnerId === g.player1Id;
  });

  // ── Cards ───────────────────────────────────────────

  async loadCards(): Promise<void> {
    try {
      const cards = await firstValueFrom(
        this.http.get<CardResponse[]>(`${this.baseUrl}/api/cards`)
      );
      const map = new Map<string, CardResponse>();
      for (const c of cards) {
        map.set(c.id, c);
      }
      this._cards.set(map);
    } catch {
      console.error('Failed to load cards');
    }
  }

  getCard(id: string): CardResponse | undefined {
    return this._cards().get(id);
  }

  // ── Create AI Game ──────────────────────────────────

  async createAiGame(request: CreateAiGameRequest): Promise<boolean> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const resp = await firstValueFrom(
        this.http.post<GameActionResponse>(`${this.baseUrl}/api/games/ai`, request)
      );
      this.applyResponse(resp);
      return true;
    } catch (e: any) {
      this._error.set(e?.error?.message ?? 'Failed to create game');
      return false;
    } finally {
      this._loading.set(false);
    }
  }

  // ── Play Card ───────────────────────────────────────

  async playCard(cardId: string, targetId?: string | null): Promise<boolean> {
    const gameId = this._game()?.id;
    if (!gameId) return false;

    this._loading.set(true);
    this._error.set(null);
    try {
      const body: PlayCardRequest = { cardId, targetId };
      const resp = await firstValueFrom(
        this.http.post<GameActionResponse>(`${this.baseUrl}/api/games/${gameId}/play-card`, body)
      );
      this.applyResponse(resp);
      return true;
    } catch (e: any) {
      this._error.set(e?.error?.message ?? 'Failed to play card');
      return false;
    } finally {
      this._loading.set(false);
    }
  }

  // ── Attack ──────────────────────────────────────────

  async attack(attackerInstanceId: string, targetId: string): Promise<boolean> {
    const gameId = this._game()?.id;
    if (!gameId) return false;

    this._loading.set(true);
    this._error.set(null);
    try {
      const body: AttackRequest = { attackerInstanceId, targetId };
      const resp = await firstValueFrom(
        this.http.post<GameActionResponse>(`${this.baseUrl}/api/games/${gameId}/attack`, body)
      );
      this.applyResponse(resp);
      return true;
    } catch (e: any) {
      this._error.set(e?.error?.message ?? 'Failed to attack');
      return false;
    } finally {
      this._loading.set(false);
    }
  }

  // ── End Turn ────────────────────────────────────────

  async endTurn(): Promise<boolean> {
    const gameId = this._game()?.id;
    if (!gameId) return false;

    this._loading.set(true);
    this._error.set(null);
    try {
      const resp = await firstValueFrom(
        this.http.post<GameActionResponse>(`${this.baseUrl}/api/games/${gameId}/end-turn`, {})
      );
      this.applyResponse(resp);
      return true;
    } catch (e: any) {
      this._error.set(e?.error?.message ?? 'Failed to end turn');
      return false;
    } finally {
      this._loading.set(false);
    }
  }

  // ── Surrender ───────────────────────────────────────

  async surrender(): Promise<boolean> {
    const gameId = this._game()?.id;
    if (!gameId) return false;

    this._loading.set(true);
    this._error.set(null);
    try {
      const resp = await firstValueFrom(
        this.http.post<GameActionResponse>(`${this.baseUrl}/api/games/${gameId}/surrender`, {})
      );
      this.applyResponse(resp);
      return true;
    } catch (e: any) {
      this._error.set(e?.error?.message ?? 'Surrender failed');
      return false;
    } finally {
      this._loading.set(false);
    }
  }

  // ── Reset ───────────────────────────────────────────

  reset(): void {
    this._game.set(null);
    this._events.set([]);
    this._error.set(null);
    this._loading.set(false);
  }

  // ── Internals ───────────────────────────────────────

  private applyResponse(resp: GameActionResponse): void {
    this._game.set(resp.game);
    this._events.update(prev => [...prev, ...resp.events]);
  }
}
