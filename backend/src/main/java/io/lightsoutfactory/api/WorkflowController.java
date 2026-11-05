package io.lightsoutfactory.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import io.lightsoutfactory.workflow.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class WorkflowController {
    private final WorkflowService workflow;

    public WorkflowController(WorkflowService workflow) {
        this.workflow = workflow;
    }

    @GetMapping("/work-items")
    public List<ApiDtos.WorkItemView> listWorkItems() {
        return workflow.listWorkItems();
    }

    @PostMapping("/work-items")
    public ResponseEntity<ApiDtos.WorkItemView> createWorkItem(
            @Valid @RequestBody ApiDtos.CreateWorkItemRequest request, HttpServletRequest httpRequest) {
        ApiDtos.WorkItemView created = workflow.createWorkItem(request, correlationId(httpRequest));
        return ResponseEntity.created(URI.create("/api/v1/work-items/" + created.id())).body(created);
    }

    @GetMapping("/work-items/{id}")
    public ApiDtos.WorkItemView getWorkItem(@PathVariable UUID id) {
        return workflow.getWorkItem(id);
    }

    @PostMapping("/work-items/{id}/plans")
    public ResponseEntity<ApiDtos.PlanView> createPlan(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApiDtos.CreatePlanRequest request,
            HttpServletRequest httpRequest) {
        ApiDtos.PlanView created = workflow.createPlan(id, request == null ? new ApiDtos.CreatePlanRequest(null) : request,
                correlationId(httpRequest));
        return ResponseEntity.created(URI.create("/api/v1/plans/" + created.id())).body(created);
    }

    @GetMapping("/plans/{id}")
    public ApiDtos.PlanView getPlan(@PathVariable UUID id) {
        return workflow.getPlan(id);
    }

    @PostMapping("/plans/{id}/approve")
    public ApiDtos.PlanView approvePlan(
            @PathVariable UUID id, @Valid @RequestBody ApiDtos.ApproveRequest request, HttpServletRequest httpRequest) {
        return workflow.approvePlan(id, request, correlationId(httpRequest));
    }

    @PostMapping("/plans/{id}/execute")
    public ApiDtos.ExecutionView executePlan(
            @PathVariable UUID id, @Valid @RequestBody ApiDtos.ExecuteRequest request, HttpServletRequest httpRequest) {
        return workflow.executePlan(id, request, correlationId(httpRequest));
    }

    @GetMapping("/executions/{id}")
    public ApiDtos.ExecutionView getExecution(@PathVariable UUID id) {
        return workflow.getExecution(id);
    }

    @PostMapping("/work-items/{id}/complete")
    public ApiDtos.WorkItemView completeWorkItem(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return workflow.completeWorkItem(id, correlationId(httpRequest));
    }

    @GetMapping("/work-items/{id}/audit-events")
    public List<ApiDtos.AuditView> getAuditEvents(@PathVariable UUID id) {
        return workflow.getAuditEvents(id);
    }

    private UUID correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        if (value instanceof UUID id) {
            return id;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Request correlation is unavailable.");
    }
}
