import { Component, input, computed } from '@angular/core';
import { manaFill } from '../animations/game.animations';

@Component({
  selector: 'app-mana-bar',
  standalone: true,
  templateUrl: './mana-bar.html',
  styleUrl: './mana-bar.scss',
  animations: [manaFill],
})
export class ManaBar {
  readonly current = input(0);
  readonly max = input(0);

  readonly crystals = computed(() => {
    const m = this.max();
    const c = this.current();
    return Array.from({ length: m }, (_, i) => i < c);
  });
}
