import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { GameCard } from '../components/game-card';
import { HeroPortrait } from '../components/hero-portrait';
import { CardResponse } from '../models/game.models';
import { CardCatalogService } from '../../library/services/card-catalog.service';

@Component({
  selector: 'app-game-info-page',
  standalone: true,
  imports: [CommonModule, GameCard, HeroPortrait],
  templateUrl: './game-info-page.html',
  styleUrl: './game-info-page.scss',
})
export class GameInfoPage implements OnInit {
  private readonly router = inject(Router);
  private readonly catalog = inject(CardCatalogService);

  readonly loading = this.catalog.loading;
  readonly error = this.catalog.error;
  readonly cards = this.catalog.cards;

  readonly exampleCards = computed<readonly CardResponse[]>(() => {
    const cards = this.cards();
    if (!cards.length) return [];

    const pick = (name: string): CardResponse | null =>
      cards.find(c => c.name.toLowerCase() === name.toLowerCase()) ?? null;

    const names = [
      'Town Guard',      // Guard
      'Razor Cub',       // Charge
      'Copper Drone',    // Ward
      'High Paladin',    // Lifesteal
      'Ice Shard',       // Freeze spell
      'Ember Bolt',      // Any-target damage
    ];

    return names
      .map(pick)
      .filter((c): c is CardResponse => c !== null);
  });

  async ngOnInit(): Promise<void> {
    await this.catalog.loadAll();
  }

  goBack(): void {
    this.router.navigateByUrl('/');
  }

  goToLibrary(): void {
    this.router.navigateByUrl('/library');
  }
}
