import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Modern Angular 18 functional interceptor (replaces the older
 * class-based HttpInterceptor pattern). Runs on EVERY outgoing HTTP
 * request made through Angular's HttpClient.
 *
 * Two jobs, same as the Axios interceptor pattern used in the earlier
 * React projects:
 *   1. Attach "Authorization: Bearer <token>" automatically — no
 *      component ever has to remember to add this header manually.
 *   2. If the backend responds 401 (token expired/invalid), clear the
 *      session and redirect to /login automatically.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error) => {
      if (error.status === 401) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
