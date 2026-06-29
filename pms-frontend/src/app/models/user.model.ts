/**
 * TypeScript interfaces are the frontend equivalent of the backend's
 * DTOs (records). They don't generate any runtime code — they exist
 * purely so the TypeScript compiler catches typos and shape mismatches
 * (e.g. `user.usernam` instead of `user.username`) at compile time
 * instead of failing silently in the browser at runtime.
 */

export type Role = 'ROLE_ADMIN' | 'ROLE_MANAGER' | 'ROLE_MEMBER';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  role?: string; // 'ADMIN' | 'MANAGER' | '' (blank = Member)
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Exactly matches the backend's LoginResponse record field-for-field. */
export interface LoginResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  role: Role;
}

/** What AuthService stores in localStorage and shares via the signal. */
export interface AuthUser {
  id: number;
  username: string;
  email: string;
  role: Role;
  token: string;
}
