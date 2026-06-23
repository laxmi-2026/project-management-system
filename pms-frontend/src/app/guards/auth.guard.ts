import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Modern Angular functional guard (CanActivateFn) — the equivalent of
 * React's <ProtectedRoute> wrapper from the earlier LMS project, but
 * implemented as a function Angular's router calls before activating
 * a route, rather than a wrapping component.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};