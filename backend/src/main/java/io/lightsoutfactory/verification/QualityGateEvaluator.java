package io.lightsoutfactory.verification;

import io.lightsoutfactory.execution.ExecutionRun;
import org.springframework.stereotype.Component;

@Component
public class QualityGateEvaluator {
    public Evaluation evaluate(ExecutionRun run) {
        boolean valid = "SUCCEEDED".equals(run.getStatus())
                && run.getOutputSummary() != null
                && run.getOutputSummary().startsWith("SIMULATED:");
        return new Evaluation(valid ? "PASSED" : "FAILED",
                valid ? "The simulated output is present and has the expected provenance prefix."
                        : "The run did not produce valid simulated output.");
    }

    public record Evaluation(String status, String evidence) {
    }
}
