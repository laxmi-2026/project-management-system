import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthUser, LoginRequest, LoginResponse, RegisterRequest, Role } from '../models/user.model';

/**
 * Central place that owns "who is logged in right now". Uses Angular
 * signals (modern Angular reactivity, similar spirit to Java 21 being
 * the modern choice on the backend) instead of older BehaviorSubject
 * patterns — any component can read currentUser() and the template
 * re-renders automatically when it changes, with no manual subscribe/
 * unsubscribe bookkeeping needed.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  // Signal holding the current user, initialized from localStorage so a
  // page refresh doesn't log the user out.
  private userSignal = signal<AuthUser | null>(this.readUserFromStorage());

  readonly currentUser = computed(() => this.userSignal());
  readonly isLoggedIn = computed(() => this.userSignal() !== null);
  readonly userRole = computed<Role | null>(() => this.userSignal()?.role ?? null);

  constructor(private http: HttpClient, private router: Router) {}

  register(request: RegisterRequest): Observable<{ message: string; email: string }> {
    return this.http.post<{ message: string; email: string }>(`${this.apiUrl}/register`, request);
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((response) => this.persistSession(response))
    );
  }

  logout(): void {
    localStorage.removeItem('pms_user');
    this.userSignal.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.userSignal()?.token ?? null;
  }

  isAdmin(): boolean {
    return this.userRole() === 'ROLE_ADMIN';
  }

  isManager(): boolean {
    return this.userRole() === 'ROLE_MANAGER';
  }

  /** True for Admin OR Manager — used to show/hide "create" buttons,
   *  since both roles can manage projects and tasks. */
  canManage(): boolean {
    return this.isAdmin() || this.isManager();
  }

  isMember(): boolean {
    return this.userRole() === 'ROLE_MEMBER';
  }

  private persistSession(response: LoginResponse): void {
    const user: AuthUser = {
      id: response.id,
      username: response.username,
      email: response.email,
      role: response.role,
      token: response.token
    };
    localStorage.setItem('pms_user', JSON.stringify(user));
    this.userSignal.set(user);
  }

  private readUserFromStorage(): AuthUser | null {
    const raw = localStorage.getItem('pms_user');
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }
}
