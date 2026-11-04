package io.lightsoutfactory.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lightsoutfactory.api.WorkflowException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentExecutorRegistryTest {
    @Test
    void resolvesMockAndRejectsUnknownProviderTypes() {
        AgentExecutor mock = new MockAgentExecutor();
        AgentExecutorRegistry registry = new AgentExecutorRegistry(List.of(mock));

        assertThat(registry.resolve("mock")).isSameAs(mock);
        assertThatThrownBy(() -> registry.resolve("not-configured"))
                .isInstanceOf(WorkflowException.class)
                .hasMessage("Unsupported agent provider.");
    }
}
