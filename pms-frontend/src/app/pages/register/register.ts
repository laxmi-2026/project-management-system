import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/**
 * Password strength meter calibrated around an 8-character minimum
 * (matching the backend's @Size(min = 8)):
 *   level 0: empty
 *   level 1: meets the bare minimum (8+ chars) — "Weak"
 *   level 2: 12+ chars — "Medium"
 *   level 3: 12+ chars AND has both an uppercase letter and a digit — "Strong"
 */
function getPasswordStrength(password: string): number {
  if (!password) return 0;
  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  if (/[A-Z]/.test(password) && /[0-9]/.test(password)) score++;
  return score;
}

const STRENGTH_CONFIG = [
  { label: '', color: '#dee2e6' },
  { label: 'Weak', color: '#dc3545' },
  { label: 'Medium', color: '#fd7e14' },
  { label: 'Strong', color: '#198754' }
];

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html'
})
export class Register {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  showPassword = false;
  showConfirm = false;
  selectedRole = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
      email: ['', [Validators.required, Validators.email]],
      // Minimum 8 characters — matches backend's @Size(min = 8) on
      // RegisterRequest exactly, so a password that passes here will
      // never bounce back with a server-side validation error.
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordsMatchValidator });
  }

  get username() { return this.form.get('username')!; }
  get email() { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }
  get confirmPassword() { return this.form.get('confirmPassword')!; }

  passwordsMatchValidator(group: FormGroup) {
    const password = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    if (password && confirm && password !== confirm) {
      group.get('confirmPassword')?.setErrors({ mismatch: true });
      return { mismatch: true };
    }
    return null;
  }

  get passwordStrength(): { label: string; level: number; color: string } {
    const level = getPasswordStrength(this.password.value || '');
    return { ...STRENGTH_CONFIG[level], level };
  }

  togglePassword(): void { this.showPassword = !this.showPassword; }
  toggleConfirm(): void { this.showConfirm = !this.showConfirm; }

  selectRole(role: string): void {
    this.selectedRole = role;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { confirmPassword, ...rest } = this.form.value;
    const payload = { ...rest, role: this.selectedRole };

    this.authService.register(payload).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading = false;
        const errors = err?.error;
        if (errors && typeof errors === 'object' && !errors.message) {
          this.errorMessage = Object.values(errors).join(', ');
        } else {
          this.errorMessage = errors?.message || 'Registration failed';
        }
      }
    });
  }
}