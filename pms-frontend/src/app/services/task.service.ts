import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PagedTaskResponse, TaskRequest, TaskResponse, TaskStatusUpdateRequest } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly apiUrl = `${environment.apiUrl}/tasks`;

  constructor(private http: HttpClient) {}

  getByProject(projectId: number): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(`${this.apiUrl}/project/${projectId}`);
  }

  getMyTasks(): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(`${this.apiUrl}/my-tasks`);
  }

  getById(id: number): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Cross-project search with optional filters and pagination, backing
   * the "All Tasks" page. The overdue flag is now a real server-side
   * filter (not a client-side post-filter), so the count shown and the
   * list returned stay consistent across pages.
   */
  searchTasks(filters: {
    status?: string;
    priority?: string;
    projectId?: number;
    assignedToUserId?: number;
    search?: string;
    overdue?: boolean;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Observable<PagedTaskResponse> {
    let params = new HttpParams();
    if (filters.status)           params = params.set('status', filters.status);
    if (filters.priority)         params = params.set('priority', filters.priority);
    if (filters.projectId)        params = params.set('projectId', filters.projectId.toString());
    if (filters.assignedToUserId) params = params.set('assignedToUserId', filters.assignedToUserId.toString());
    if (filters.search)           params = params.set('search', filters.search);
    if (filters.overdue)          params = params.set('overdue', 'true');
    params = params.set('page', (filters.page ?? 0).toString());
    params = params.set('size', (filters.size ?? 10).toString());
    params = params.set('sortBy', filters.sortBy ?? 'createdAt');
    params = params.set('sortDir', filters.sortDir ?? 'desc');

    return this.http.get<PagedTaskResponse>(this.apiUrl, { params });
  }

  create(request: TaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(this.apiUrl, request);
  }

  update(id: number, request: TaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`${this.apiUrl}/${id}`, request);
  }

  updateStatus(id: number, request: TaskStatusUpdateRequest): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.apiUrl}/${id}/status`, request);
  }

  delete(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/${id}`);
  }
}