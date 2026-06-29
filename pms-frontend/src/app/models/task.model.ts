export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface TaskRequest {
  title: string;
  description?: string;
  dueDate?: string;
  priority?: TaskPriority;
  status?: TaskStatus;
  projectId: number;
  assignedToUserId?: number;
}

export interface TaskStatusUpdateRequest {
  status: TaskStatus;
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  dueDate: string | null;
  priority: TaskPriority;
  status: TaskStatus;
  projectId: number;
  projectName: string;
  assignedToUserId: number | null;
  assignedToUsername: string | null;
  createdByUsername: string | null;
  createdAt: string;
  updatedAt: string;
  overdue: boolean;
}

/** Wraps a page of tasks with pagination metadata — used by the new
 *  cross-project "All Tasks" page. */
export interface PagedTaskResponse {
  content: TaskResponse[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}