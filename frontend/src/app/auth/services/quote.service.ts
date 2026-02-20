import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../../api/api-base-url.token';

export type QuoteResponse = {
  text: string;
};

@Injectable({ providedIn: 'root' })
export class QuoteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  async getRandomQuote(): Promise<QuoteResponse | null> {
    const resp = await firstValueFrom(
      this.http.get<QuoteResponse>(`${this.baseUrl}/api/quotes/random`, {
        observe: 'response'
      })
    );

    if (resp.status === 204) {
      return null;
    }

    return resp.body ?? null;
  }
}
