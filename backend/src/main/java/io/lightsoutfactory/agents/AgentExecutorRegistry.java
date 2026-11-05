package io.lightsoutfactory.agents;

import io.lightsoutfactory.api.WorkflowException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AgentExecutorRegistry {
    private final Map<String, AgentExecutor> executors;

    public AgentExecutorRegistry(List<AgentExecutor> executors) {
        this.executors = executors.stream().collect(Collectors.toUnmodifiableMap(
                AgentExecutor::providerType, Function.identity()));
    }

    public AgentExecutor resolve(String providerType) {
        AgentExecutor executor = executors.get(providerType);
        if (executor == null) {
            throw new WorkflowException(HttpStatus.BAD_REQUEST, "Unsupported agent provider.");
        }
        return executor;
    }
}
