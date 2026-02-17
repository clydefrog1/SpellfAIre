import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../../api/api-base-url.token';
import type {
  AuthResponse,
  LoginRequest,
  RefreshResponse,
  RegisterRequest,
  UserResponse
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  readonly accessToken = signal<string | null>(null);
  readonly user = signal<UserResponse | null>(null);
  readonly initialized = signal(false);

  private initializingPromise: Promise<void> | null = null;

  async ensureInitialized(): Promise<void> {
    if (this.initialized()) {
      return;
    }
    if (this.initializingPromise) {
      return this.initializingPromise;
    }

    this.initializingPromise = (async () => {
      try {
        await this.refresh();
      } catch {
        // Not logged in (or refresh invalid).
      } finally {
        this.initialized.set(true);
      }
    })();

    return this.initializingPromise;
  }

  async register(payload: RegisterRequest): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<AuthResponse>(`${this.apiBaseUrl}/api/auth/register`, payload, {
        withCredentials: true
      })
    );
    this.accessToken.set(res.accessToken);
    this.user.set(res.user);
  }

  async login(payload: LoginRequest): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<AuthResponse>(`${this.apiBaseUrl}/api/auth/login`, payload, {
        withCredentials: true
      })
    );
    this.accessToken.set(res.accessToken);
    this.user.set(res.user);
  }

  async refresh(): Promise<void> {
    const refreshed = await firstValueFrom(
      this.http.post<RefreshResponse>(`${this.apiBaseUrl}/api/auth/refresh`, {}, { withCredentials: true })
    );
    this.accessToken.set(refreshed.accessToken);

    const me = await firstValueFrom(this.http.get<UserResponse>(`${this.apiBaseUrl}/api/auth/me`));
    this.user.set(me);
  }

  async logout(): Promise<void> {
    await firstValueFrom(
      this.http.post<void>(`${this.apiBaseUrl}/api/auth/logout`, {}, { withCredentials: true })
    );
    this.accessToken.set(null);
    this.user.set(null);
  }
}
