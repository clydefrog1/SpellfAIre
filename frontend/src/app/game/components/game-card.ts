import { Component, input, output, computed } from '@angular/core';
import { CardResponse } from '../models/game.models';

@Component({
  selector: 'app-game-card',
  standalone: true,
  templateUrl: './game-card.html',
  styleUrl: './game-card.scss',
})
export class GameCard {
  readonly card = input.required<CardResponse>();
  readonly playable = input(false);
  readonly selected = input(false);
  readonly cardClick = output<CardResponse>();

  readonly isCreature = computed(() => this.card().cardType === 'CREATURE');
  readonly isSpell = computed(() => this.card().cardType === 'SPELL');
  readonly cardClass = computed(() => {
    const c = this.card();
    if (c.faction) return `faction-${c.faction.toLowerCase()}`;
    if (c.school) return `school-${c.school.toLowerCase()}`;
    return '';
  });

  onClick(): void {
    if (this.playable()) {
      this.cardClick.emit(this.card());
    }
  }
}
