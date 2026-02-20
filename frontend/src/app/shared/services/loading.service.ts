import { DOCUMENT } from '@angular/common';
import { inject, Injectable, computed, effect, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly document = inject(DOCUMENT);

  private readonly pendingCount = signal(0);

  readonly active = computed(() => this.pendingCount() > 0);

  constructor() {
    effect(() => {
      const isActive = this.active();
      this.document.documentElement.classList.toggle('sf-loading', isActive);
    });
  }

  begin(): void {
    this.pendingCount.update(count => count + 1);
  }

  end(): void {
    this.pendingCount.update(count => Math.max(0, count - 1));
  }
}
