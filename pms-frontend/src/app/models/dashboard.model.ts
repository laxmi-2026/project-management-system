import { TaskResponse } from './task.model';

/** Exactly matches the backend's DashboardStats record. */
export interface DashboardStats {
  totalProjects: number;
  activeProjects: number;
  completedProjects: number;
  totalTasks: number;
  completedTasks: number;
  overdueTasks: number;
  tasksByStatus: { [key: string]: number }; // { "TODO": 5, "IN_PROGRESS": 3, "DONE": 12 }
  myRecentTasks: TaskResponse[];
}