import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
import { ProjectResponse } from '../../models/project.model';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './projects.html'
})
export class Projects implements OnInit {
  projects: ProjectResponse[] = [];
  loading = true;
  showForm = false;
  editingId: number | null = null;
  submitting = false;
  errorMessage = '';

  /** Bound to both date inputs' [min] attribute — disables past dates
   *  directly in the browser's native date picker. */
  todayDateString = new Date().toISOString().split('T')[0];

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private projectService: ProjectService,
    public auth: AuthService
  ) {
    this.form = this.fb.group({
      // Letters, spaces, hyphens, apostrophes only — matches the
      // backend's @Pattern on ProjectRequest.name exactly, so a name
      // that passes here will never bounce back with a server error.
      name: ['', [
        Validators.required,
        Validators.maxLength(100),
        Validators.pattern(/^[a-zA-Z\s\-']+$/)
      ]],
      description: ['', [Validators.maxLength(2000)]],
      startDate: [''],
      endDate: [''],
      status: ['PLANNING']
    }, { validators: this.dateRangeValidator });
  }

  ngOnInit(): void {
    this.loadProjects();
  }

  get name() { return this.form.get('name')!; }

  dateRangeValidator(group: FormGroup) {
    const start = group.get('startDate')?.value;
    const end = group.get('endDate')?.value;
    if (start && end && new Date(end) < new Date(start)) {
      group.get('endDate')?.setErrors({ beforeStart: true });
      return { beforeStart: true };
    }
    return null;
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getAll().subscribe({
      next: (data) => {
        this.projects = data;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  openCreateForm(): void {
    this.form.reset({ status: 'PLANNING' });
    this.editingId = null;
    this.showForm = true;
    this.errorMessage = '';
  }

  openEditForm(project: ProjectResponse): void {
    this.form.patchValue({
      name: project.name,
      description: project.description,
      startDate: project.startDate,
      endDate: project.endDate,
      status: project.status
    });
    this.editingId = project.id;
    this.showForm = true;
    this.errorMessage = '';
  }

  closeForm(): void {
    this.showForm = false;
    this.editingId = null;
    this.form.reset({ status: 'PLANNING' });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    const payload = this.form.value;

    const request$ = this.editingId
      ? this.projectService.update(this.editingId, payload)
      : this.projectService.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting = false;
        this.closeForm();
        this.loadProjects();
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err?.error?.message || 'Something went wrong. Please try again.';
      }
    });
  }

  deleteProject(project: ProjectResponse): void {
    if (!confirm(`Delete "${project.name}"?\n\nThis cannot be undone.`)) return;

    this.projectService.delete(project.id).subscribe({
      next: () => this.loadProjects(),
      error: (err) => {
        alert(err?.error?.message || 'Delete failed. The project may still have incomplete tasks.');
      }
    });
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-success';
      case 'COMPLETED': return 'bg-primary';
      case 'ON_HOLD': return 'bg-warning text-dark';
      default: return 'bg-secondary';
    }
  }
}