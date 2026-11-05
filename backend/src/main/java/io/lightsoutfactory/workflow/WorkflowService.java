package io.lightsoutfactory.workflow;

import io.lightsoutfactory.agents.AgentExecutor;
import io.lightsoutfactory.agents.AgentExecutorRegistry;
import io.lightsoutfactory.api.ApiDtos;
import io.lightsoutfactory.api.WorkflowException;
import io.lightsoutfactory.audit.AuditEvent;
import io.lightsoutfactory.audit.AuditEventRepository;
import io.lightsoutfactory.execution.ExecutionRun;
import io.lightsoutfactory.execution.ExecutionRunRepository;
import io.lightsoutfactory.observability.FactoryMetrics;
import io.lightsoutfactory.planning.AgentTask;
import io.lightsoutfactory.planning.AgentTaskRepository;
import io.lightsoutfactory.planning.ExecutionPlan;
import io.lightsoutfactory.planning.ExecutionPlanRepository;
import io.lightsoutfactory.verification.QualityGateEvaluator;
import io.lightsoutfactory.verification.QualityGateResult;
import io.lightsoutfactory.verification.QualityGateResultRepository;
import io.lightsoutfactory.workitem.WorkItem;
import io.lightsoutfactory.workitem.WorkItemRepository;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

@Service
public class WorkflowService {
    private static final String DEMO_ACTOR = "local-demo";
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkItemRepository workItems;
    private final ExecutionPlanRepository plans;
    private final AgentTaskRepository tasks;
    private final ExecutionRunRepository runs;
    private final QualityGateResultRepository gates;
    private final AuditEventRepository audit;
    private final AgentExecutorRegistry executors;
    private final QualityGateEvaluator gateEvaluator;
    private final FactoryMetrics metrics;
    private final ObservationRegistry observations;

    public WorkflowService(
            WorkItemRepository workItems,
            ExecutionPlanRepository plans,
            AgentTaskRepository tasks,
            ExecutionRunRepository runs,
            QualityGateResultRepository gates,
            AuditEventRepository audit,
            AgentExecutorRegistry executors,
            QualityGateEvaluator gateEvaluator,
            FactoryMetrics metrics,
            ObservationRegistry observations) {
        this.workItems = workItems;
        this.plans = plans;
        this.tasks = tasks;
        this.runs = runs;
        this.gates = gates;
        this.audit = audit;
        this.executors = executors;
        this.gateEvaluator = gateEvaluator;
        this.metrics = metrics;
        this.observations = observations;
    }

    @Transactional
    public ApiDtos.WorkItemView createWorkItem(ApiDtos.CreateWorkItemRequest request, UUID correlationId) {
        return observe("factory.work_item.create", correlationId, null, () -> {
            String priority = request.priority() == null ? "P2" : request.priority();
            WorkItem item = persist("work_item.save", correlationId, null,
                    () -> workItems.save(new WorkItem(request.title().trim(), request.description().trim(),
                            request.acceptanceCriteria().trim(), priority)));
            metrics.workItemCreated();
            record(item, "WORK_ITEM_CREATED", "work-item", item.getId(), correlationId);
            return ApiDtos.WorkItemView.from(item, null, null, null);
        });
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.WorkItemView> listWorkItems() {
        return workItems.findAllByOrderByCreatedAtDesc().stream().map(this::workItemView).toList();
    }

    @Transactional(readOnly = true)
    public ApiDtos.WorkItemView getWorkItem(UUID id) {
        WorkItem item = requireWorkItem(id);
        return workItemView(item);
    }

    @Transactional
    public ApiDtos.PlanView createPlan(UUID workItemId, ApiDtos.CreatePlanRequest request, UUID correlationId) {
        return observe("factory.workflow.plan", correlationId, workItemId, () -> {
            WorkItem item = requireWorkItem(workItemId);
            if (item.getStatus() != WorkflowState.CREATED
                    || plans.findFirstByWorkItem_IdOrderByCreatedAtDesc(workItemId).isPresent()) {
                throw conflict("A plan can only be created once for a newly created work item.");
            }
            String summary = request.summary() == null || request.summary().isBlank()
                    ? "Address the work item and verify each acceptance criterion." : request.summary().trim();
            ExecutionPlan plan = persist("execution_plan.save", correlationId, item.getId(),
                    () -> plans.save(new ExecutionPlan(item, summary)));
            persist("agent_task.save", correlationId, item.getId(),
                    () -> tasks.save(new AgentTask(plan, item.getTitle(), item.getAcceptanceCriteria())));
            item.transitionTo(WorkflowState.AWAITING_APPROVAL);
            record(item, "PLAN_CREATED", "plan", plan.getId(), correlationId);
            return ApiDtos.PlanView.from(plan);
        });
    }

    @Transactional(readOnly = true)
    public ApiDtos.PlanView getPlan(UUID id) {
        return ApiDtos.PlanView.from(requirePlan(id));
    }

    @Transactional
    public ApiDtos.PlanView approvePlan(UUID planId, ApiDtos.ApproveRequest request, UUID correlationId) {
        return observe("factory.workflow.approve", correlationId, null, () -> {
            ExecutionPlan plan = requirePlan(planId);
            WorkItem item = plan.getWorkItem();
            if (!"AWAITING_APPROVAL".equals(plan.getStatus())
                    || item.getStatus() != WorkflowState.AWAITING_APPROVAL) {
                throw conflict("Only a plan awaiting approval can be approved.");
            }
            plan.approve(request.actor().trim());
            item.transitionTo(WorkflowState.APPROVED);
            record(item, "PLAN_APPROVED", "plan", planId, correlationId, request.actor().trim());
            return ApiDtos.PlanView.from(plan);
        });
    }

    @Transactional
    public ApiDtos.ExecutionView executePlan(UUID planId, ApiDtos.ExecuteRequest request, UUID correlationId) {
        ExecutionPlan observedPlan = requirePlan(planId);
        return observe("factory.workflow.execute", correlationId, observedPlan.getWorkItem().getId(),
                () -> executeApprovedPlan(observedPlan, request, correlationId));
    }

    private ApiDtos.ExecutionView executeApprovedPlan(
            ExecutionPlan plan, ApiDtos.ExecuteRequest request, UUID correlationId) {
        WorkItem item = plan.getWorkItem();
        if (!"APPROVED".equals(plan.getStatus()) || item.getStatus() != WorkflowState.APPROVED) {
            throw conflict("Plan approval is required before execution.");
        }

        AgentTask task = tasks.findFirstByPlan_Id(plan.getId())
                .orElseThrow(() -> new IllegalStateException("Plan has no task."));
        task.start();
        plan.markExecuting();
        item.transitionTo(WorkflowState.EXECUTING);
        ExecutionRun run = persist("execution_run.save", correlationId, item.getId(),
                () -> runs.save(new ExecutionRun(task, request.providerType(), correlationId)));
        Instant executionStartedAt = Instant.now();
        metrics.executionStarted();
        record(item, "RUN_STARTED", "execution", run.getId(), correlationId);

        AgentExecutor executor = executors.resolve(request.providerType());
        AgentExecutor.Result result = observe("factory.agent.execute", correlationId, item.getId(),
                () -> executor.execute(task.getTitle(), request.scenario()));
        run.complete(result.status(), result.output(), result.errorCode());
        item.transitionTo(WorkflowState.VERIFYING);
        String runAction = switch (result.status()) {
            case "SUCCEEDED" -> "RUN_FINISHED";
            case "TIMED_OUT" -> "RUN_TIMED_OUT";
            default -> "RUN_FAILED";
        };
        record(item, runAction, "execution", run.getId(), correlationId);

        QualityGateEvaluator.Evaluation evaluation = observe(
                "factory.quality_gate.evaluate", correlationId, item.getId(), () -> gateEvaluator.evaluate(run));
        QualityGateResult gate = persist("quality_gate.save", correlationId, item.getId(),
                () -> gates.save(new QualityGateResult(run, evaluation.status(), evaluation.evidence())));
        boolean passed = "PASSED".equals(evaluation.status());
        task.finish(passed);
        plan.markFinished(passed);
        item.transitionTo(passed ? WorkflowState.REVIEW_REQUIRED : WorkflowState.FAILED);
        record(item, passed ? "QUALITY_GATE_PASSED" : "QUALITY_GATE_FAILED",
                "quality-gate", gate.getId(), correlationId);
        metrics.executionFinished(result.status(), gate.getStatus(),
                Duration.between(executionStartedAt, Instant.now()), task.getAttemptCount());
        return ApiDtos.ExecutionView.from(run, gate);
    }

    @Transactional(readOnly = true)
    public ApiDtos.ExecutionView getExecution(UUID id) {
        ExecutionRun run = runs.findById(id)
                .orElseThrow(() -> new WorkflowException(HttpStatus.NOT_FOUND, "Execution was not found."));
        QualityGateResult gate = gates.findFirstByExecutionRun_Id(id)
                .orElseThrow(() -> new IllegalStateException("Execution has no quality gate result."));
        return ApiDtos.ExecutionView.from(run, gate);
    }

    @Transactional
    public ApiDtos.WorkItemView completeWorkItem(UUID workItemId, UUID correlationId) {
        WorkItem item = requireWorkItem(workItemId);
        if (item.getStatus() != WorkflowState.REVIEW_REQUIRED) {
            throw conflict("Only work items with a passing quality gate can be completed.");
        }
        item.transitionTo(WorkflowState.COMPLETED);
        record(item, "WORK_ITEM_COMPLETED", "work-item", item.getId(), correlationId);
        return workItemView(item);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.AuditView> getAuditEvents(UUID workItemId) {
        requireWorkItem(workItemId);
        return audit.findByWorkItemIdOrderByOccurredAtAsc(workItemId).stream()
                .map(ApiDtos.AuditView::from).toList();
    }

    private WorkItem requireWorkItem(UUID id) {
        return workItems.findById(id)
                .orElseThrow(() -> new WorkflowException(HttpStatus.NOT_FOUND, "Work item was not found."));
    }

    private ExecutionPlan requirePlan(UUID id) {
        return plans.findById(id)
                .orElseThrow(() -> new WorkflowException(HttpStatus.NOT_FOUND, "Plan was not found."));
    }

    private WorkflowException conflict(String message) {
        return new WorkflowException(HttpStatus.CONFLICT, message);
    }

    private void record(WorkItem item, String action, String resourceType, UUID resourceId, UUID correlationId) {
        record(item, action, resourceType, resourceId, correlationId, DEMO_ACTOR);
    }

    private ApiDtos.WorkItemView workItemView(WorkItem item) {
        ExecutionPlan plan = plans.findFirstByWorkItem_IdOrderByCreatedAtDesc(item.getId()).orElse(null);
        if (plan == null) {
            return ApiDtos.WorkItemView.from(item, null, null, null);
        }
        UUID runId = runs.findFirstByAgentTask_Plan_IdOrderByStartedAtDesc(plan.getId())
                .map(ExecutionRun::getId).orElse(null);
        return ApiDtos.WorkItemView.from(item, plan.getId(), plan.getStatus(), runId);
    }

    private void record(
            WorkItem item, String action, String resourceType, UUID resourceId, UUID correlationId, String actor) {
        persist("audit_event.save", correlationId, item.getId(),
                () -> audit.save(new AuditEvent(actor, action, resourceType, resourceId, item.getId(), correlationId)));
        LOGGER.atInfo()
                .addKeyValue("component", "workflow")
                .addKeyValue("event", action)
                .addKeyValue("workItemId", item.getId())
                .addKeyValue("executionId", "execution".equals(resourceType) ? resourceId : null)
                .addKeyValue("correlationId", correlationId)
                .log("Workflow event");
    }

    private <T> T observe(String name, UUID correlationId, UUID workItemId, Supplier<T> operation) {
        Observation observation = Observation.createNotStarted(name, observations)
                .lowCardinalityKeyValue("workflow.operation", name);
        if (correlationId != null) {
            observation.highCardinalityKeyValue("correlation.id", correlationId.toString());
        }
        if (workItemId != null) {
            observation.highCardinalityKeyValue("work.item.id", workItemId.toString());
        }
        return observation.observe(operation);
    }

    private <T> T persist(String operation, UUID correlationId, UUID workItemId, Supplier<T> action) {
        return observe("factory.persistence." + operation, correlationId, workItemId, action);
    }
}
