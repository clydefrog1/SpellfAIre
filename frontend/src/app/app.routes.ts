import { Routes } from '@angular/router';

import { authGuard } from './auth/guards/auth.guard';
import { HomePage } from './auth/pages/home-page';
import { LoginPage } from './auth/pages/login-page';
import { ProfilePage } from './auth/pages/profile-page';
import { RegisterPage } from './auth/pages/register-page';

export const routes: Routes = [
	{ path: '', component: HomePage, canActivate: [authGuard] },
	{ path: 'login', component: LoginPage },
	{ path: 'register', component: RegisterPage },
	{ path: 'profile', component: ProfilePage, canActivate: [authGuard] },
	{
		path: 'library',
		loadComponent: () => import('./library/pages/card-library-page').then(m => m.CardLibraryPage),
		canActivate: [authGuard],
	},
	{
		path: 'game-info',
		loadComponent: () => import('./game/pages/game-info-page').then(m => m.GameInfoPage),
		canActivate: [authGuard],
	},
	{
		path: 'game',
		loadChildren: () => import('./game/game.routes').then(m => m.gameRoutes),
	},
	{ path: '**', redirectTo: 'login' }
];
