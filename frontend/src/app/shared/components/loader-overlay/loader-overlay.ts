import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { AuthService } from '../../../auth/services/auth.service';
import { LoadingService } from '../../services/loading.service';

@Component({
  selector: 'sf-loader-overlay',
  templateUrl: './loader-overlay.html',
  styleUrl: './loader-overlay.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoaderOverlayComponent {
  private readonly auth = inject(AuthService);
  private readonly loading = inject(LoadingService);

  readonly label = input<string>('Consulting the runes…');

  readonly active = computed(() => !this.auth.initialized() || this.loading.active());
}
