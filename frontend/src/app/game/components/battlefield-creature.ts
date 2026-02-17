import { Component, input, output, computed } from '@angular/core';
import { BoardCreatureResponse, CardResponse, Keyword } from '../models/game.models';

@Component({
  selector: 'app-battlefield-creature',
  standalone: true,
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
