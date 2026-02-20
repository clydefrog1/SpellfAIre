import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { QuoteService } from '../services/quote.service';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss'
})
export class HomePage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly quotes = inject(QuoteService);

  readonly user = this.auth.user;

  readonly immersiveQuote = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      const q = await this.quotes.getRandomQuote();
      this.immersiveQuote.set(q?.text ?? null);
    } catch {
      // Quote is non-essential UI.
      this.immersiveQuote.set(null);
    }
  }

  async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }

  playVsAi(): void {
    this.router.navigateByUrl('/game/setup');
  }

  goToProfile(): void {
    this.router.navigateByUrl('/profile');
  }

  goToLibrary(): void {
    this.router.navigateByUrl('/library');
  }
}
