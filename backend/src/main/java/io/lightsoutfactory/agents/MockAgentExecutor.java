package io.lightsoutfactory.agents;

import org.springframework.stereotype.Component;

@Component
public class MockAgentExecutor implements AgentExecutor {
    @Override
    public String providerType() {
        return "mock";
    }

    @Override
    public Result execute(String taskTitle, String scenario) {
        return switch (scenario == null ? "SUCCESS" : scenario) {
            case "SUCCESS" -> new Result("SUCCEEDED", "SIMULATED: completed task \"" + taskTitle + "\".", null);
            case "FAILURE" -> new Result("FAILED", null, "MOCK_FAILURE");
            case "TIMEOUT" -> new Result("TIMED_OUT", null, "MOCK_TIMEOUT");
            default -> throw new IllegalArgumentException("Unsupported mock scenario");
        };
    }
}
