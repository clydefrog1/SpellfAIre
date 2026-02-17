import { Routes } from '@angular/router';
import { authGuard } from '../auth/guards/auth.guard';

export const gameRoutes: Routes = [
  {
    path: 'setup',
    loadComponent: () => import('./pages/game-setup').then(m => m.GameSetup),
    canActivate: [authGuard],
  },
  {
    path: 'board',
    loadComponent: () => import('./pages/game-board').then(m => m.GameBoard),
    canActivate: [authGuard],
  },
  {
    path: '',
    redirectTo: 'setup',
    pathMatch: 'full',
  },
];
