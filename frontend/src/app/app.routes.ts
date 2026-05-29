import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { adminGuard } from './core/admin.guard';

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
  {
    path: 'audit',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/audit-list.component').then((m) => m.AuditListComponent),
  },
  {
    path: 'users',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/users-list.component').then((m) => m.UsersListComponent),
  },
  {
    path: 'users/new',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/user-form.component').then((m) => m.UserFormComponent),
  },
  {
    path: 'users/:id/edit',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/user-form.component').then((m) => m.UserFormComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'flags' },
  { path: '**', redirectTo: 'flags' },
];
