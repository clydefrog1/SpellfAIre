import { DecimalPipe } from '@angular/common';
import { Component, input, computed } from '@angular/core';
import { GameEvent } from '../models/game.models';
import { logEntry } from '../animations/game.animations';

@Component({
  selector: 'app-game-event-log',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './game-event-log.html',
  styleUrl: './game-event-log.scss',
  animations: [logEntry],
})
export class GameEventLog {
  readonly events = input<GameEvent[]>([]);

  /** Show last 12 events */
  readonly recentEvents = computed(() => {
    const all = this.events();
    return all.slice(Math.max(0, all.length - 12));
  });

  getIcon(type: GameEvent['type']): string {
    switch (type) {
      case 'DAMAGE': return '💥';
      case 'HEAL': return '💚';
      case 'DEATH': return '💀';
      case 'CARD_PLAYED': return '🃏';
      case 'CARD_DRAWN': return '📤';
      case 'SPELL_RESOLVED': return '✨';
      case 'ATTACK': return '⚔️';
      case 'FATIGUE': return '😵';
      case 'BUFF': return '⬆️';
      case 'FREEZE': return '❄️';
      case 'GAME_OVER': return '🏆';
      case 'SUMMON': return '🌟';
      case 'TURN_START': return '🔄';
      case 'MANA_GAIN': return '💎';
      default: return '📝';
    }
  }
}
