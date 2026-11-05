package io.lightsoutfactory.agents;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MockAgentExecutorTest {
    private final MockAgentExecutor executor = new MockAgentExecutor();

    @Test
    void modelsSuccessFailureAndTimeoutWithoutRunningExternalWork() {
        assertThat(executor.execute("Build a health endpoint", "SUCCESS"))
                .satisfies(result -> {
                    assertThat(result.status()).isEqualTo("SUCCEEDED");
                    assertThat(result.output()).startsWith("SIMULATED:");
                    assertThat(result.errorCode()).isNull();
                });
        assertThat(executor.execute("Build a health endpoint", "FAILURE").errorCode()).isEqualTo("MOCK_FAILURE");
        assertThat(executor.execute("Build a health endpoint", "TIMEOUT").status()).isEqualTo("TIMED_OUT");
    }
}
