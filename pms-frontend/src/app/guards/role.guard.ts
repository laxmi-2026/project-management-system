import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Blocks a route unless the logged-in user is Admin or Manager.
 * Used on routes like "create project" or "create task" forms — a
 * Member who manually types the URL gets redirected to /dashboard
 * instead of seeing a form they have no backend permission to submit
 * anyway (the backend's @PreAuthorize would reject it with 403, but
 * blocking it here gives a much better user experience than letting
 * them fill out a form just to get an error on submit).
 */
export const managerOrAdminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.canManage()) {
    return true;
  }

  router.navigate(['/dashboard']);
  return false;
};
