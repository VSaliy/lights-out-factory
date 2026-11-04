package io.lightsoutfactory.workflow;

import io.lightsoutfactory.agents.AgentExecutor;
import io.lightsoutfactory.agents.AgentExecutorRegistry;
import io.lightsoutfactory.api.ApiDtos;
import io.lightsoutfactory.api.WorkflowException;
import io.lightsoutfactory.audit.AuditEvent;
import io.lightsoutfactory.audit.AuditEventRepository;
import io.lightsoutfactory.execution.ExecutionRun;
import io.lightsoutfactory.execution.ExecutionRunRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {
    private static final String DEMO_ACTOR = "local-demo";

    private final WorkItemRepository workItems;
    private final ExecutionPlanRepository plans;
    private final AgentTaskRepository tasks;
    private final ExecutionRunRepository runs;
    private final QualityGateResultRepository gates;
    private final AuditEventRepository audit;
    private final AgentExecutorRegistry executors;
    private final QualityGateEvaluator gateEvaluator;

    public WorkflowService(
            WorkItemRepository workItems,
            ExecutionPlanRepository plans,
            AgentTaskRepository tasks,
            ExecutionRunRepository runs,
            QualityGateResultRepository gates,
            AuditEventRepository audit,
            AgentExecutorRegistry executors,
            QualityGateEvaluator gateEvaluator) {
        this.workItems = workItems;
        this.plans = plans;
        this.tasks = tasks;
        this.runs = runs;
        this.gates = gates;
        this.audit = audit;
        this.executors = executors;
        this.gateEvaluator = gateEvaluator;
    }

    @Transactional
    public ApiDtos.WorkItemView createWorkItem(ApiDtos.CreateWorkItemRequest request, UUID correlationId) {
        String priority = request.priority() == null ? "P2" : request.priority();
        WorkItem item = workItems.save(new WorkItem(request.title().trim(), request.description().trim(),
                request.acceptanceCriteria().trim(), priority));
        record(item, "WORK_ITEM_CREATED", "work-item", item.getId(), correlationId);
        return ApiDtos.WorkItemView.from(item, null, null, null);
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
        WorkItem item = requireWorkItem(workItemId);
        if (item.getStatus() != WorkflowState.CREATED
                || plans.findFirstByWorkItem_IdOrderByCreatedAtDesc(workItemId).isPresent()) {
            throw conflict("A plan can only be created once for a newly created work item.");
        }
        String summary = request.summary() == null || request.summary().isBlank()
                ? "Address the work item and verify each acceptance criterion." : request.summary().trim();
        ExecutionPlan plan = plans.save(new ExecutionPlan(item, summary));
        tasks.save(new AgentTask(plan, item.getTitle(), item.getAcceptanceCriteria()));
        item.transitionTo(WorkflowState.AWAITING_APPROVAL);
        record(item, "PLAN_CREATED", "plan", plan.getId(), correlationId);
        return ApiDtos.PlanView.from(plan);
    }

    @Transactional(readOnly = true)
    public ApiDtos.PlanView getPlan(UUID id) {
        return ApiDtos.PlanView.from(requirePlan(id));
    }

    @Transactional
    public ApiDtos.PlanView approvePlan(UUID planId, ApiDtos.ApproveRequest request, UUID correlationId) {
        ExecutionPlan plan = requirePlan(planId);
        WorkItem item = plan.getWorkItem();
        if (!"AWAITING_APPROVAL".equals(plan.getStatus()) || item.getStatus() != WorkflowState.AWAITING_APPROVAL) {
            throw conflict("Only a plan awaiting approval can be approved.");
        }
        plan.approve(request.actor().trim());
        item.transitionTo(WorkflowState.APPROVED);
        record(item, "PLAN_APPROVED", "plan", planId, correlationId, request.actor().trim());
        return ApiDtos.PlanView.from(plan);
    }

    @Transactional
    public ApiDtos.ExecutionView executePlan(UUID planId, ApiDtos.ExecuteRequest request, UUID correlationId) {
        ExecutionPlan plan = requirePlan(planId);
        WorkItem item = plan.getWorkItem();
        if (!"APPROVED".equals(plan.getStatus()) || item.getStatus() != WorkflowState.APPROVED) {
            throw conflict("Plan approval is required before execution.");
        }

        AgentTask task = tasks.findFirstByPlan_Id(planId)
                .orElseThrow(() -> new IllegalStateException("Plan has no task."));
        task.start();
        plan.markExecuting();
        item.transitionTo(WorkflowState.EXECUTING);
        ExecutionRun run = runs.save(new ExecutionRun(task, request.providerType(), correlationId));
        record(item, "RUN_STARTED", "execution", run.getId(), correlationId);

        AgentExecutor.Result result = executors.resolve(request.providerType()).execute(task.getTitle(), request.scenario());
        run.complete(result.status(), result.output(), result.errorCode());
        item.transitionTo(WorkflowState.VERIFYING);
        record(item, "RUN_FINISHED", "execution", run.getId(), correlationId);

        QualityGateEvaluator.Evaluation evaluation = gateEvaluator.evaluate(run);
        QualityGateResult gate = gates.save(new QualityGateResult(run, evaluation.status(), evaluation.evidence()));
        boolean passed = "PASSED".equals(evaluation.status());
        task.finish(passed);
        plan.markFinished(passed);
        item.transitionTo(passed ? WorkflowState.REVIEW_REQUIRED : WorkflowState.FAILED);
        record(item, passed ? "QUALITY_GATE_PASSED" : "QUALITY_GATE_FAILED",
                "quality-gate", gate.getId(), correlationId);
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
        audit.save(new AuditEvent(actor, action, resourceType, resourceId, item.getId(), correlationId));
    }
}
