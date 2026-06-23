import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { TaskService } from '../../services/task.service';
import { AuthService } from '../../services/auth.service';
import { TaskResponse, TaskStatus } from '../../models/task.model';

@Component({
  selector: 'app-kanban-board',
  standalone: true,
  imports: [CommonModule, RouterLink, DragDropModule],
  templateUrl: './kanban-board.html'
})
export class KanbanBoard implements OnInit {
  projectId!: number;
  loading = true;

  todoTasks: TaskResponse[] = [];
  inProgressTasks: TaskResponse[] = [];
  doneTasks: TaskResponse[] = [];

  /** Same read-only detail modal pattern used on Project Detail —
   *  clicking a card here opens it instead of leaving the board with
   *  no way to see a task's description/assignee without navigating
   *  away to the table view. */
  viewingTask: TaskResponse | null = null;

  constructor(
    private route: ActivatedRoute,
    private taskService: TaskService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.taskService.getByProject(this.projectId).subscribe({
      next: (tasks) => {
        this.todoTasks       = tasks.filter(t => t.status === 'TODO');
        this.inProgressTasks = tasks.filter(t => t.status === 'IN_PROGRESS');
        this.doneTasks       = tasks.filter(t => t.status === 'DONE');
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onDrop(event: CdkDragDrop<TaskResponse[]>, newStatus: TaskStatus): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    const task = event.previousContainer.data[event.previousIndex];

    transferArrayItem(
      event.previousContainer.data,
      event.container.data,
      event.previousIndex,
      event.currentIndex
    );

    this.taskService.updateStatus(task.id, { status: newStatus }).subscribe({
      error: () => {
        transferArrayItem(
          event.container.data,
          event.previousContainer.data,
          event.currentIndex,
          event.previousIndex
        );
        alert('Failed to update task status. Please try again.');
      }
    });
  }

  openViewModal(task: TaskResponse): void {
    this.viewingTask = task;
  }

  closeViewModal(): void {
    this.viewingTask = null;
  }

  /** Returns v2 badge modifier classes — consistent with every other
   *  page in the app (Project Detail, All Tasks). */
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