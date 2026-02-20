import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../../api/api-base-url.token';
import { CardResponse } from '../../game/models/game.models';

@Injectable({ providedIn: 'root' })
export class CardCatalogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  readonly cards = signal<readonly CardResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async loadAll(options?: { force?: boolean }): Promise<void> {
    const force = options?.force ?? false;
    if (!force && this.cards().length > 0) return;

    this.loading.set(true);
    this.error.set(null);
    try {
      const cards = await firstValueFrom(
        this.http.get<CardResponse[]>(`${this.baseUrl}/api/cards`)
      );
      this.cards.set(cards);
    } catch {
      this.error.set('Failed to load cards');
    } finally {
      this.loading.set(false);
    }
  }
}
