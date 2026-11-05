import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WorkItem {
  id: string;
  title: string;
  description: string;
  acceptanceCriteria: string;
  priority: string;
  status: string;
  latestPlanId: string | null;
  latestExecutionId: string | null;
}

export interface CreateWorkItem {
  title: string;
  description: string;
  acceptanceCriteria: string;
  priority: string;
}

export interface Plan {
  id: string;
  status: string;
}

export interface Execution {
  id: string;
  providerType: string;
  status: string;
  simulated: boolean;
  outputSummary: string | null;
  errorCode: string | null;
  correlationId: string;
  gateStatus: string;
  gateEvidence: string;
}

export interface AuditEvent {
  id: string;
  occurredAt: string;
  actor: string;
  action: string;
}

@Injectable({ providedIn: 'root' })
export class FactoryApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  listWorkItems(): Observable<WorkItem[]> {
    return this.http.get<WorkItem[]>(`${this.base}/work-items`);
  }

  createWorkItem(request: CreateWorkItem): Observable<WorkItem> {
    return this.http.post<WorkItem>(`${this.base}/work-items`, request);
  }

  createPlan(workItemId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.base}/work-items/${workItemId}/plans`, {});
  }

  approve(planId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.base}/plans/${planId}/approve`, { actor: 'local-demo' });
  }

  execute(planId: string): Observable<Execution> {
    return this.http.post<Execution>(`${this.base}/plans/${planId}/execute`, {
      providerType: 'mock',
      scenario: 'SUCCESS',
    });
  }

  getExecution(id: string): Observable<Execution> {
    return this.http.get<Execution>(`${this.base}/executions/${id}`);
  }

  complete(workItemId: string): Observable<WorkItem> {
    return this.http.post<WorkItem>(`${this.base}/work-items/${workItemId}/complete`, {});
  }

  audit(workItemId: string): Observable<AuditEvent[]> {
    return this.http.get<AuditEvent[]>(`${this.base}/work-items/${workItemId}/audit-events`);
  }
}
