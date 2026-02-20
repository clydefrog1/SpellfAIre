import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../../api/api-base-url.token';
import { AuthService } from './auth.service';
import type { UpdateProfileRequest, UserResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly auth = inject(AuthService);

  async updateProfile(request: UpdateProfileRequest): Promise<UserResponse> {
    const updated = await firstValueFrom(
      this.http.patch<UserResponse>(`${this.apiBaseUrl}/api/users/me`, request)
    );
    this.auth.user.set(updated);
    return updated;
  }
}
