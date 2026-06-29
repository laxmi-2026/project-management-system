export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED';

/** Sent to the backend when creating or updating a project. No `id` field —
 *  matches the backend's ProjectRequest exactly, for the same security
 *  reason: never let the client dictate which row gets modified via body. */
export interface ProjectRequest {
  name: string;
  description?: string;
  startDate?: string;  // ISO date string, e.g. "2026-01-01"
  endDate?: string;
  status?: ProjectStatus;
}

/** Exactly matches the backend's ProjectResponse record. */
export interface ProjectResponse {
  id: number;
  name: string;
  description: string | null;
  startDate: string | null;
  endDate: string | null;
  status: ProjectStatus;
  progressPercent: number;
  createdByUsername: string | null;
  createdAt: string;
  totalTasks: number;
  completedTasks: number;
}