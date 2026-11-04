package io.lightsoutfactory.verification;

import static org.assertj.core.api.Assertions.assertThat;

import io.lightsoutfactory.execution.ExecutionRun;
import io.lightsoutfactory.planning.AgentTask;
import io.lightsoutfactory.planning.ExecutionPlan;
import io.lightsoutfactory.workitem.WorkItem;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QualityGateEvaluatorTest {
    @Test
    void rejectsSuccessfulAgentResponseWithoutSimulatedOutputProvenance() {
        WorkItem item = new WorkItem("Health endpoint", "Expose health", "HTTP 200", "P2");
        ExecutionPlan plan = new ExecutionPlan(item, "Build and test");
        AgentTask task = new AgentTask(plan, "Implement endpoint", "HTTP 200");
        ExecutionRun run = new ExecutionRun(task, "mock", UUID.randomUUID());
        run.complete("SUCCEEDED", "Agent says it succeeded", null);

        QualityGateEvaluator.Evaluation result = new QualityGateEvaluator().evaluate(run);

        assertThat(result.status()).isEqualTo("FAILED");
    }
}
