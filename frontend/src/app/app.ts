import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { FactoryApi, AuditEvent, Execution, WorkItem } from './factory-api';

@Component({
  selector: 'app-root',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly api = inject(FactoryApi);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  protected readonly items = signal<WorkItem[]>([]);
  protected readonly events = signal<AuditEvent[]>([]);
  protected readonly execution = signal<Execution | null>(null);
  protected readonly selectedId = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly form = this.formBuilder.group({
    title: ['', [Validators.required, Validators.maxLength(160)]],
    description: ['', [Validators.required, Validators.maxLength(4000)]],
    acceptanceCriteria: ['', [Validators.required, Validators.maxLength(4000)]],
    priority: ['P2'],
  });

  constructor() {
    this.reload();
  }

  protected count(status: string): number {
    return this.items().filter((item) => item.status === status).length;
  }

  protected reload(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.listWorkItems().subscribe({
      next: (items) => {
        this.items.set(items);
        this.loading.set(false);
        const selected = items.find((item) => item.id === this.selectedId()) ?? items[0];
        if (selected) this.inspect(selected);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Could not load work items. Check that the API is running.');
      },
    });
  }

  protected createWorkItem(): void {
    if (this.form.invalid || this.busy()) return;
    this.runAction(this.api.createWorkItem(this.form.getRawValue()), () => {
      this.form.reset({ title: '', description: '', acceptanceCriteria: '', priority: 'P2' });
      this.reload();
    });
  }

  protected createPlan(item: WorkItem): void {
    this.runAction(this.api.createPlan(item.id), () => this.reload());
  }

  protected approve(item: WorkItem): void {
    if (!item.latestPlanId) return;
    this.runAction(this.api.approve(item.latestPlanId), () => this.reload());
  }

  protected execute(item: WorkItem): void {
    if (!item.latestPlanId) return;
    this.runAction(this.api.execute(item.latestPlanId), (result) => {
      this.execution.set(result);
      this.selectedId.set(item.id);
      this.reload();
    });
  }

  protected complete(item: WorkItem): void {
    this.runAction(this.api.complete(item.id), () => this.reload());
  }

  protected inspect(item: WorkItem): void {
    this.selectedId.set(item.id);
    this.events.set([]);
    this.execution.set(null);
    this.api.audit(item.id).subscribe({
      next: (events) => this.events.set(events),
      error: () => this.error.set('Could not load the audit history.'),
    });
    if (item.latestExecutionId) {
      this.api.getExecution(item.latestExecutionId).subscribe({
        next: (execution) => this.execution.set(execution),
        error: () => this.error.set('Could not load execution evidence.'),
      });
    }
  }

  private runAction<T>(request: Observable<T>, success: (value: T) => void): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    request.subscribe({
      next: (value) => {
        this.busy.set(false);
        success(value);
      },
      error: (problem: { error?: { detail?: string } }) => {
        this.busy.set(false);
        this.error.set(problem.error?.detail ?? 'The request could not be completed.');
        this.reload();
      },
    });
  }
}
