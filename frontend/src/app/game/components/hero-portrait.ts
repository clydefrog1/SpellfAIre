import { Component, input, output, computed } from '@angular/core';
import { ManaBar } from './mana-bar';

@Component({
  selector: 'app-hero-portrait',
  standalone: true,
  templateUrl: './hero-portrait.html',
  styleUrl: './hero-portrait.scss',
  imports: [ManaBar],
})
export class HeroPortrait {
  readonly health = input(25);
  readonly maxHealth = input(25);
  readonly side = input<'player' | 'opponent'>('player');
  readonly highlighted = input(false);
  readonly currentMana = input(0);
  readonly maxMana = input(0);
  readonly deckCount = input(0);
  readonly heroClick = output<void>();

  readonly healthPercent = computed(() =>
    Math.max(0, Math.round((this.health() / this.maxHealth()) * 100))
  );

  readonly isDamaged = computed(() => this.health() < this.maxHealth());
  readonly isCritical = computed(() => this.health() <= 8);

  onClick(): void {
    this.heroClick.emit();
  }
}
