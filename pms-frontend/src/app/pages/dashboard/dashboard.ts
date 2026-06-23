import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { DashboardService } from '../../services/dashboard.service';
import { AuthService } from '../../services/auth.service';
import { DashboardStats } from '../../models/dashboard.model';
import { TaskResponse } from '../../models/task.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html'
})
export class Dashboard implements OnInit {
  stats: DashboardStats | null = null;
  loading = true;
  greeting = '';

  constructor(
    private dashboardService: DashboardService,
    private router: Router,
    public auth: AuthService
  ) {
    const hour = new Date().getHours();
    this.greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  }

  ngOnInit(): void {
    this.dashboardService.getStats().subscribe({
      next: (data) => { this.stats = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  statusCount(status: string): number {
    return this.stats?.tasksByStatus?.[status] ?? 0;
  }

  goToProjects(): void {
    this.router.navigate(['/projects']);
  }

  goToAllTasks(): void {
    this.router.navigate(['/tasks']);
  }

  goToTasksFiltered(status: string): void {
    this.router.navigate(['/tasks'], { queryParams: { status } });
  }

  /** "Overdue" stat card now links to a properly server-side-filtered
   *  view (?overdue=true), instead of the unfiltered All Tasks list —
   *  this was the actual bug: the card itself was always correct,
   *  but clicking it didn't carry the filter through. */
  goToOverdueTasks(): void {
    this.router.navigate(['/tasks'], { queryParams: { overdue: 'true' } });
  }

  openTask(task: TaskResponse): void {
    this.router.navigate(['/projects', task.projectId]);
  }
}