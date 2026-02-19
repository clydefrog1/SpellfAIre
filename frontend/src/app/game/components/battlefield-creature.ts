import { Component, input, output, computed } from '@angular/core';
import { GameCard } from './game-card';
import { BoardCreatureResponse, CardResponse, Keyword } from '../models/game.models';

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
  selector: 'app-battlefield-creature',
  standalone: true,
  imports: [GameCard],
  templateUrl: './battlefield-creature.html',
  styleUrl: './battlefield-creature.scss',
})
export class BattlefieldCreature {
  readonly creature = input.required<BoardCreatureResponse>();
  readonly card = input<CardResponse | undefined>();
  readonly side = input<'player' | 'opponent'>('player');
  readonly selected = input(false);
  readonly selectable = input(false);
  readonly highlighted = input(false);
  readonly attackSourceToken = input<number | null>(null);
  readonly attackTargetToken = input<number | null>(null);
  readonly hitFx = input<CombatHitFx | null>(null);
  readonly floatFx = input<CombatFloatFx | null>(null);
  readonly spellFx = input<SpellImpactFx | null>(null);
  readonly creatureClick = output<BoardCreatureResponse>();

  readonly name = computed(() => this.card()?.name ?? '???');

  readonly hasGuard = computed(() =>
    this.creature().keywords?.includes('GUARD' as Keyword)
  );
  readonly hasLifesteal = computed(() =>
    this.creature().keywords?.includes('LIFESTEAL' as Keyword)
  );
  readonly hasWard = computed(() =>
    this.creature().keywords?.includes('WARD' as Keyword)
  );
  readonly isFrozen = computed(() =>
    this.creature().statuses?.includes('FROZEN')
  );
  readonly isDamaged = computed(() =>
    this.creature().health < this.creature().maxHealth
  );

  readonly isAttackReady = computed(() => {
    const c = this.creature();
    return this.side() === 'player' && c.canAttack && !c.hasAttackedThisTurn && !this.isFrozen();
  });

  readonly isAttackSource = computed(() => this.attackSourceToken() !== null);
  readonly isAttackSourceOdd = computed(() => {
    const token = this.attackSourceToken();
    return token !== null && token % 2 === 1;
  });
  readonly isAttackSourceEven = computed(() => {
    const token = this.attackSourceToken();
    return token !== null && token % 2 === 0;
  });

  readonly isAttackTargeted = computed(() => this.attackTargetToken() !== null);
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

  getTooltip(): string {
    const c = this.creature();
    const cardName = this.name();
    
    if (this.side() === 'player') {
      if (c.canAttack && !c.hasAttackedThisTurn && !this.isFrozen()) {
        return `${cardName} - Click to select for attack`;
      }
      if (c.hasAttackedThisTurn) {
        return `${cardName} - Already attacked this turn`;
      }
      if (this.isFrozen()) {
        return `${cardName} - Frozen (cannot attack)`;
      }
      if (!c.canAttack) {
        return `${cardName} - Cannot attack yet (summoning sickness)`;
      }
    }
    
    return `${cardName} - ${c.attack}/${c.health}`;
  }

  onClick(): void {
    this.creatureClick.emit(this.creature());
  }
}
