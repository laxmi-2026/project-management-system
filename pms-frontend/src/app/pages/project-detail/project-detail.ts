import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, ValidatorFn, Validators, AbstractControl } from '@angular/forms';
import { ProjectService } from '../../services/project.service';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../services/auth.service';
import { ProjectResponse } from '../../models/project.model';
import { TaskResponse } from '../../models/task.model';

/** Same rule as the backend's @FutureOrPresent on TaskRequest.dueDate. */
function notInPastValidator(): ValidatorFn {
  return (control: AbstractControl) => {
    if (!control.value) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const selected = new Date(control.value);
    selected.setHours(0, 0, 0, 0);
    return selected < today ? { pastDate: true } : null;
  };
}

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './project-detail.html'
})
export class ProjectDetail implements OnInit {
  project: ProjectResponse | null = null;
  tasks: TaskResponse[] = [];
  loading = true;
  showForm = false;
  editingTaskId: number | null = null;
  submitting = false;
  errorMessage = '';
  projectId!: number;

  viewingTask: TaskResponse | null = null;

  todayDateString = new Date().toISOString().split('T')[0];

  form: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private projectService: ProjectService,
    private taskService: TaskService,
    public auth: AuthService
  ) {
    this.form = this.fb.group({
      // Letters, spaces, hyphens, apostrophes only — matches the
      // backend's @Pattern on TaskRequest.title exactly.
      title: ['', [
        Validators.required,
        Validators.maxLength(150),
        Validators.pattern(/^[a-zA-Z\s\-']+$/)
      ]],
      description: ['', [Validators.maxLength(2000)]],
      dueDate: ['', [notInPastValidator()]],
      priority: ['MEDIUM'],
      status: ['TODO']
    });
  }

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadProject();
    this.loadTasks();
  }

  get title() { return this.form.get('title')!; }
  get dueDate() { return this.form.get('dueDate')!; }

  loadProject(): void {
    this.projectService.getById(this.projectId).subscribe({
      next: (data) => { this.project = data; },
      error: () => { this.router.navigate(['/projects']); }
    });
  }

  loadTasks(): void {
    this.loading = true;
    this.taskService.getByProject(this.projectId).subscribe({
      next: (data) => { this.tasks = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  goBack(): void {
    this.router.navigate(['/projects']);
  }

  openCreateForm(): void {
    this.form.reset({ priority: 'MEDIUM', status: 'TODO' });
    this.editingTaskId = null;
    this.showForm = true;
    this.errorMessage = '';
  }

  openEditForm(task: TaskResponse): void {
    this.form.patchValue({
      title: task.title,
      description: task.description,
      dueDate: task.dueDate,
      priority: task.priority,
      status: task.status
    });
    this.editingTaskId = task.id;
    this.showForm = true;
    this.errorMessage = '';
  }

  closeForm(): void {
    this.showForm = false;
    this.editingTaskId = null;
  }

  openViewModal(task: TaskResponse): void {
    this.viewingTask = task;
  }

  closeViewModal(): void {
    this.viewingTask = null;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const dueDateValue = this.form.value.dueDate;
    if (dueDateValue && this.project?.startDate && new Date(dueDateValue) < new Date(this.project.startDate)) {
      this.errorMessage = `Due date cannot be before the project's start date (${this.project.startDate})`;
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    const payload = { ...this.form.value, projectId: this.projectId };

    const request$ = this.editingTaskId
      ? this.taskService.update(this.editingTaskId, payload)
      : this.taskService.create(payload);

    request$.subscribe({
      next: () => {
        this.submitting = false;
        this.closeForm();
        this.loadTasks();
        this.loadProject();
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err?.error?.message || 'Failed to save task';
      }
    });
  }

  deleteTask(task: TaskResponse): void {
    if (!confirm(`Delete task "${task.title}"?`)) return;

    this.taskService.delete(task.id).subscribe({
      next: () => { this.loadTasks(); this.loadProject(); },
      error: (err) => alert(err?.error?.message || 'Delete failed')
    });
  }

  priorityBadgeClass(priority: string): string {
    switch (priority) {
      case 'HIGH': return 'onhold';
      case 'MEDIUM': return 'active';
      default: return 'planning';
    }
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'DONE': return 'completed';
      case 'IN_PROGRESS': return 'onhold';
      default: return 'planning';
    }
  }
}