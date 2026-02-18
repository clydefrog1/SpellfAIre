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
  /** True when the card costs more mana than the player currently has. */
  readonly insufficientMana = input(false);
  readonly cardClick = output<CardResponse>();

  readonly isCreature = computed(() => this.card().cardType === 'CREATURE');
  readonly isSpell = computed(() => this.card().cardType === 'SPELL');

  readonly playableTip = computed(() =>
    this.card().cardType === 'CREATURE' ? 'Summon to Battlefield' : 'Cast this spell'
  );

  readonly keywords = computed(() => {
    const maybeKeywords = (this.card() as unknown as { keywords?: string[] | null }).keywords;
    return Array.isArray(maybeKeywords) ? maybeKeywords : [];
  });

  readonly monogram = computed(() => {
    const rawName = this.card().name?.trim() ?? '';
    if (!rawName) return '??';
    const parts = rawName.split(/\s+/g).filter(Boolean);
    if (parts.length === 1) {
      const word = parts[0].toUpperCase();
      return word.slice(0, 2);
    }
    return (parts[0][0] + parts[1][0]).toUpperCase();
  });
  readonly cardClass = computed(() => {
    const c = this.card();
    if (c.faction) return `faction-${c.faction.toLowerCase()}`;
    if (c.school) return `school-${c.school.toLowerCase()}`;
    return '';
  });

  keywordTitle(keyword: string): string {
    switch (keyword) {
      case 'GUARD':
        return 'Guard — Enemies must target Guard creatures first with creature attacks.';
      case 'CHARGE':
        return 'Charge — This creature can attack the turn it is played.';
      case 'LIFESTEAL':
        return 'Lifesteal — Damage this creature deals heals your Hero for the same amount.';
      case 'WARD':
        return 'Ward — The first time this creature would take damage, prevent it and remove Ward.';
      default:
        return String(keyword);
    }
  }

  onClick(): void {
    if (this.playable()) {
      this.cardClick.emit(this.card());
    }
  }
}
