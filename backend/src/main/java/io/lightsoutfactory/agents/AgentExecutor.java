package io.lightsoutfactory.agents;

public interface AgentExecutor {
    String providerType();
    Result execute(String taskTitle, String scenario);

    record Result(String status, String output, String errorCode) {
    }
}
