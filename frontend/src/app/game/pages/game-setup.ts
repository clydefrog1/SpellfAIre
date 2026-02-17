import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { GameService } from '../services/game.service';
import {
  Faction,
  MagicSchool,
  FACTIONS,
  SCHOOLS,
  FactionInfo,
  SchoolInfo,
} from '../models/game.models';
import { listStagger } from '../animations/game.animations';

@Component({
  selector: 'app-game-setup',
  standalone: true,
  templateUrl: './game-setup.html',
  styleUrl: './game-setup.scss',
  animations: [listStagger],
})
export class GameSetup {
  private readonly gameService = inject(GameService);
  private readonly router = inject(Router);

  readonly factions = FACTIONS;
  readonly schools = SCHOOLS;

  readonly selectedFaction = signal<Faction | null>(null);
  readonly selectedSchool = signal<MagicSchool | null>(null);
  readonly loading = this.gameService.loading;
  readonly error = this.gameService.error;

  selectFaction(f: FactionInfo): void {
    this.selectedFaction.set(f.id);
  }

  selectSchool(s: SchoolInfo): void {
    this.selectedSchool.set(s.id);
  }

  async startGame(): Promise<void> {
    const faction = this.selectedFaction();
    const school = this.selectedSchool();
    if (!faction || !school) return;

    await this.gameService.loadCards();
    const ok = await this.gameService.createAiGame({ faction, magicSchool: school });
    if (ok) {
      await this.router.navigateByUrl('/game/board');
    }
  }

  goBack(): void {
    this.router.navigateByUrl('/');
  }
}
