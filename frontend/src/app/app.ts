import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AuthService } from './auth/services/auth.service';
import { LoadingService } from './shared/services/loading.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly loading = inject(LoadingService);
  protected readonly title = signal('spellfaire');

  async ngOnInit(): Promise<void> {
    this.loading.begin();
    try {
      await this.auth.ensureInitialized();
    } finally {
      this.loading.end();
    }
  }
}
