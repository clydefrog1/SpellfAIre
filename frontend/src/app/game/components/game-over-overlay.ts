import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-game-over-overlay',
  standalone: true,
  templateUrl: './game-over-overlay.html',
  styleUrl: './game-over-overlay.scss',
})
export class GameOverOverlay {
  readonly won = input<boolean | null>(null);
  readonly exit = output<void>();
  readonly playAgain = output<void>();

  onExit(): void {
    this.exit.emit();
  }

  onPlayAgain(): void {
    this.playAgain.emit();
  }
}
