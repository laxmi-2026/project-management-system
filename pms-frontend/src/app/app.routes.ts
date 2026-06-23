import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then(m => m.Login)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register').then(m => m.Register)
  },

  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard),
    canActivate: [authGuard]
  },

  {
    path: 'tasks',
    loadComponent: () => import('./pages/all-tasks/all-tasks').then(m => m.AllTasks),
    canActivate: [authGuard]
  },

  {
    path: 'projects',
    loadComponent: () => import('./pages/projects/projects').then(m => m.Projects),
    canActivate: [authGuard]
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./pages/project-detail/project-detail').then(m => m.ProjectDetail),
    canActivate: [authGuard]
  },
  {
    path: 'projects/:id/board',
    loadComponent: () => import('./pages/kanban-board/kanban-board').then(m => m.KanbanBoard),
    canActivate: [authGuard]
  },

  { path: '**', redirectTo: 'dashboard' }
];