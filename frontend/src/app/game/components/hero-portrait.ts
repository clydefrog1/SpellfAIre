import { Component, input, output, computed } from '@angular/core';
import { ManaBar } from './mana-bar';

export interface CombatHitFx {
  kind: 'damage' | 'heal';
  token: number;
}

export interface CombatFloatFx {
  kind: 'damage' | 'heal';
  value: number;
  token: number;
}

export interface SpellImpactFx {
  kind: 'benefit' | 'harm';
  token: number;
}

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
  readonly disabled = input(false);
  readonly attentionToken = input<number | null>(null);
  readonly attackTargetToken = input<number | null>(null);
  readonly hitFx = input<CombatHitFx | null>(null);
  readonly floatFx = input<CombatFloatFx | null>(null);
  readonly spellFx = input<SpellImpactFx | null>(null);
  readonly currentMana = input(0);
  readonly maxMana = input(0);
  readonly deckCount = input(0);
  readonly heroClick = output<void>();
  readonly disabledClick = output<void>();

  readonly healthPercent = computed(() =>
    Math.max(0, Math.round((this.health() / this.maxHealth()) * 100))
  );

  readonly isDamaged = computed(() => this.health() < this.maxHealth());
  readonly isCritical = computed(() => this.health() <= 8);

  readonly isAttackTargeted = computed(() => this.attackTargetToken() !== null);

  readonly isAttentionEven = computed(() => {
    const token = this.attentionToken();
    return token !== null && token % 2 === 0;
  });
  readonly isAttentionOdd = computed(() => {
    const token = this.attentionToken();
    return token !== null && token % 2 === 1;
  });
  readonly isAttackTargetedOdd = computed(() => {
    const token = this.attackTargetToken();
    return token !== null && token % 2 === 1;
  });
  readonly isAttackTargetedEven = computed(() => {
    const token = this.attackTargetToken();
    return token !== null && token % 2 === 0;
  });

  readonly hitOdd = computed(() => {
    const fx = this.hitFx();
    return fx !== null && fx.token % 2 === 1;
  });
  readonly hitEven = computed(() => {
    const fx = this.hitFx();
    return fx !== null && fx.token % 2 === 0;
  });

  readonly floatOdd = computed(() => {
    const fx = this.floatFx();
    return fx !== null && fx.token % 2 === 1;
  });
  readonly floatEven = computed(() => {
    const fx = this.floatFx();
    return fx !== null && fx.token % 2 === 0;
  });

  readonly spellOdd = computed(() => {
    const fx = this.spellFx();
    return fx !== null && fx.token % 2 === 1;
  });
  readonly spellEven = computed(() => {
    const fx = this.spellFx();
    return fx !== null && fx.token % 2 === 0;
  });

  onClick(): void {
    if (this.disabled()) {
      this.disabledClick.emit();
      return;
    }
    this.heroClick.emit();
  }
}
