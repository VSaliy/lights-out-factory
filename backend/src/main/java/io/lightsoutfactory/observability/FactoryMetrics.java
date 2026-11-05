package io.lightsoutfactory.observability;

import io.lightsoutfactory.workitem.WorkItemRepository;
import io.lightsoutfactory.workflow.WorkflowState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FactoryMetrics {
    private static final List<WorkflowState> ACTIVE_STATES = List.of(
            WorkflowState.CREATED,
            WorkflowState.AWAITING_APPROVAL,
            WorkflowState.APPROVED,
            WorkflowState.EXECUTING,
            WorkflowState.VERIFYING,
            WorkflowState.REVIEW_REQUIRED);

    private final Counter workItemsCreated;
    private final Counter executionsStarted;
    private final Counter executionsCompleted;
    private final Counter executionsFailed;
    private final Counter retries;
    private final Counter qualityGateFailures;
    private final Timer executionDuration;

    public FactoryMetrics(MeterRegistry registry, WorkItemRepository workItems) {
        this.workItemsCreated = Counter.builder("factory.work.items.created")
                .description("Work items created since application start").register(registry);
        this.executionsStarted = Counter.builder("factory.executions.started")
                .description("Mock execution runs started").register(registry);
        this.executionsCompleted = Counter.builder("factory.executions.completed")
                .description("Executions with a successful agent result and passing quality gate")
                .register(registry);
        this.executionsFailed = Counter.builder("factory.executions.failed")
                .description("Executions with an unsuccessful result or failed quality gate").register(registry);
        this.retries = Counter.builder("factory.executions.retries")
                .description("Execution retries; zero while retry behavior is not implemented").register(registry);
        this.qualityGateFailures = Counter.builder("factory.quality.gates.failed")
                .description("Quality gate evaluations that failed").register(registry);
        this.executionDuration = Timer.builder("factory.executions.duration")
                .description("Time from mock execution start through quality evaluation")
                .publishPercentileHistogram()
                .register(registry);

        registry.gauge("factory.workflows.active", workItems,
                repository -> repository.countByStatusIn(ACTIVE_STATES));
    }

    public void workItemCreated() {
        workItemsCreated.increment();
    }

    public void executionStarted() {
        executionsStarted.increment();
    }

    public void executionFinished(String agentStatus, String gateStatus, Duration duration, int attemptCount) {
        if (attemptCount > 1) {
            retries.increment(attemptCount - 1);
        }
        if (!"PASSED".equals(gateStatus)) {
            qualityGateFailures.increment();
        }
        if ("SUCCEEDED".equals(agentStatus) && "PASSED".equals(gateStatus)) {
            executionsCompleted.increment();
        } else {
            executionsFailed.increment();
        }
        executionDuration.record(duration.isNegative() ? Duration.ZERO : duration);
    }
}
