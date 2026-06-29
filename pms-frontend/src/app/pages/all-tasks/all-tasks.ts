import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { TaskService } from '../../services/task.service';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
import { TaskResponse } from '../../models/task.model';
import { ProjectResponse } from '../../models/project.model';

@Component({
  selector: 'app-all-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './all-tasks.html'
})
export class AllTasks implements OnInit {
  tasks: TaskResponse[] = [];
  projects: ProjectResponse[] = [];
  loading = true;

  statusFilter = '';
  priorityFilter = '';
  projectFilter: number | null = null;
  searchTerm = '';
  /** True only when the dashboard's "Overdue" card linked in here via
   *  ?overdue=true — drives both the server-side filter and a visible
   *  "Overdue tasks" banner so the user knows why the list looks the
   *  way it does. */
  overdueOnly = false;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  private searchDebounce: any;

  constructor(
    private taskService: TaskService,
    private projectService: ProjectService,
    private router: Router,
    private route: ActivatedRoute,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const queryStatus = this.route.snapshot.queryParamMap.get('status');
    if (queryStatus) this.statusFilter = queryStatus;

    const queryOverdue = this.route.snapshot.queryParamMap.get('overdue');
    if (queryOverdue === 'true') this.overdueOnly = true;

    this.loadProjects();
    this.loadTasks();
  }

  loadProjects(): void {
    this.projectService.getAll().subscribe({
      next: (data) => { this.projects = data; }
    });
  }

  loadTasks(): void {
    this.loading = true;
    this.taskService.searchTasks({
      status: this.statusFilter || undefined,
      priority: this.priorityFilter || undefined,
      projectId: this.projectFilter || undefined,
      search: this.searchTerm || undefined,
      overdue: this.overdueOnly || undefined,
      page: this.currentPage,
      size: this.pageSize
    }).subscribe({
      next: (result) => {
        this.tasks = result.content;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadTasks();
  }

  onSearchInput(): void {
    clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => this.onFilterChange(), 400);
  }

  clearFilters(): void {
    this.statusFilter = '';
    this.priorityFilter = '';
    this.projectFilter = null;
    this.searchTerm = '';
    this.overdueOnly = false;
    this.onFilterChange();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadTasks();
  }

  get pageNumbers(): number[] {
    const window = 2;
    const start = Math.max(0, this.currentPage - window);
    const end = Math.min(this.totalPages - 1, this.currentPage + window);
    const pages: number[] = [];
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  openProject(projectId: number): void {
    this.router.navigate(['/projects', projectId]);
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