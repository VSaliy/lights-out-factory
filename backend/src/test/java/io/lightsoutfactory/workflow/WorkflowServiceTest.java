package io.lightsoutfactory.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.lightsoutfactory.agents.AgentExecutorRegistry;
import io.lightsoutfactory.agents.MockAgentExecutor;
import io.lightsoutfactory.api.ApiDtos;
import io.lightsoutfactory.api.WorkflowException;
import io.lightsoutfactory.audit.AuditEventRepository;
import io.lightsoutfactory.execution.ExecutionRunRepository;
import io.lightsoutfactory.planning.AgentTaskRepository;
import io.lightsoutfactory.planning.ExecutionPlan;
import io.lightsoutfactory.planning.ExecutionPlanRepository;
import io.lightsoutfactory.verification.QualityGateEvaluator;
import io.lightsoutfactory.verification.QualityGateResultRepository;
import io.lightsoutfactory.workitem.WorkItem;
import io.lightsoutfactory.workitem.WorkItemRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WorkflowServiceTest {
    @Test
    void executionIsRejectedUntilPlanIsApproved() {
        WorkItemRepository workItems = mock(WorkItemRepository.class);
        ExecutionPlanRepository plans = mock(ExecutionPlanRepository.class);
        AgentTaskRepository tasks = mock(AgentTaskRepository.class);
        ExecutionRunRepository runs = mock(ExecutionRunRepository.class);
        QualityGateResultRepository gates = mock(QualityGateResultRepository.class);
        AuditEventRepository audit = mock(AuditEventRepository.class);
        MockAgentExecutor executor = mock(MockAgentExecutor.class);
        when(executor.providerType()).thenReturn("mock");
        AgentExecutorRegistry registry = new AgentExecutorRegistry(java.util.List.of(executor));
        WorkflowService service = new WorkflowService(workItems, plans, tasks, runs, gates, audit,
                registry, new QualityGateEvaluator());
        WorkItem item = new WorkItem("Health endpoint", "Expose health", "HTTP 200", "P2");
        ExecutionPlan plan = new ExecutionPlan(item, "Build and test");
        when(plans.findById(plan.getId())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.executePlan(plan.getId(),
                new ApiDtos.ExecuteRequest("mock", "SUCCESS"), UUID.randomUUID()))
                .isInstanceOf(WorkflowException.class)
                .satisfies(error -> assertThat(((WorkflowException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(executor, never()).execute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(tasks, runs, gates, audit);
    }
}
