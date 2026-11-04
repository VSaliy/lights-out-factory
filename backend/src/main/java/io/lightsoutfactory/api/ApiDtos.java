package io.lightsoutfactory.api;

import io.lightsoutfactory.audit.AuditEvent;
import io.lightsoutfactory.execution.ExecutionRun;
import io.lightsoutfactory.planning.ExecutionPlan;
import io.lightsoutfactory.verification.QualityGateResult;
import io.lightsoutfactory.workitem.WorkItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record CreateWorkItemRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotBlank @Size(max = 4000) String acceptanceCriteria,
            @Pattern(regexp = "P[1-3]") String priority) {
    }

    public record CreatePlanRequest(@Size(max = 1000) String summary) {
    }

    public record ApproveRequest(@NotBlank @Size(max = 80) String actor) {
    }

    public record ExecuteRequest(
            @NotBlank @Size(max = 32) String providerType,
            @Pattern(regexp = "SUCCESS|FAILURE|TIMEOUT") String scenario) {
    }

    public record WorkItemView(
            UUID id, String title, String description, String acceptanceCriteria, String priority,
            String status, Instant createdAt, Instant updatedAt, UUID latestPlanId,
            String latestPlanStatus, UUID latestExecutionId) {
        public static WorkItemView from(WorkItem item, UUID latestPlanId, String latestPlanStatus, UUID latestExecutionId) {
            return new WorkItemView(item.getId(), item.getTitle(), item.getDescription(),
                    item.getAcceptanceCriteria(), item.getPriority(), item.getStatus().name(),
                    item.getCreatedAt(), item.getUpdatedAt(), latestPlanId, latestPlanStatus, latestExecutionId);
        }
    }

    public record PlanView(
            UUID id, UUID workItemId, String summary, String status, String approvedBy,
            Instant approvedAt, Instant createdAt) {
        public static PlanView from(ExecutionPlan plan) {
            return new PlanView(plan.getId(), plan.getWorkItem().getId(), plan.getSummary(),
                    plan.getStatus(), plan.getApprovedBy(), plan.getApprovedAt(), plan.getCreatedAt());
        }
    }

    public record ExecutionView(
            UUID id, UUID planId, String providerType, String status, boolean simulated,
            String outputSummary, String errorCode, UUID correlationId, Instant startedAt,
            Instant completedAt, String gateName, String gateStatus, String gateEvidence,
            Instant gateEvaluatedAt) {
        public static ExecutionView from(ExecutionRun run, QualityGateResult gate) {
            return new ExecutionView(run.getId(), run.getAgentTask().getPlan().getId(),
                    run.getProviderType(), run.getStatus(), run.isSimulated(), run.getOutputSummary(),
                    run.getErrorCode(), run.getCorrelationId(), run.getStartedAt(), run.getCompletedAt(),
                    gate.getGateName(), gate.getStatus(), gate.getEvidenceSummary(), gate.getEvaluatedAt());
        }
    }

    public record AuditView(
            UUID id, Instant occurredAt, String actor, String action, String resourceType,
            UUID resourceId, UUID correlationId) {
        public static AuditView from(AuditEvent event) {
            return new AuditView(event.getId(), event.getOccurredAt(), event.getActor(), event.getAction(),
                    event.getResourceType(), event.getResourceId(), event.getCorrelationId());
        }
    }
}
