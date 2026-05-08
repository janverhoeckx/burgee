import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'flags',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/flags-list.component').then((m) => m.FlagsListComponent),
  },
  {
    path: 'flags/new',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/flag-form.component').then((m) => m.FlagFormComponent),
  },
  {
    path: 'flags/:id/edit',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/flag-form.component').then((m) => m.FlagFormComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'flags' },
  { path: '**', redirectTo: 'flags' },
];
