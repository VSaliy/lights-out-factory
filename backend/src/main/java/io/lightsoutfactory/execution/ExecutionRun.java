package io.lightsoutfactory.execution;

import io.lightsoutfactory.planning.AgentTask;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_runs")
public class ExecutionRun {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_task_id", nullable = false)
    private AgentTask agentTask;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private boolean simulated;

    @Column(name = "output_summary", length = 2000)
    private String outputSummary;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ExecutionRun() {
    }

    public ExecutionRun(AgentTask task, String providerType, UUID correlationId) {
        this.id = UUID.randomUUID();
        this.agentTask = task;
        this.providerType = providerType;
        this.correlationId = correlationId;
        this.status = "RUNNING";
        this.simulated = true;
    }

    @PrePersist
    void onCreate() { this.startedAt = Instant.now(); }

    public void complete(String status, String output, String errorCode) {
        this.status = status;
        this.outputSummary = output;
        this.errorCode = errorCode;
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public AgentTask getAgentTask() { return agentTask; }
    public String getProviderType() { return providerType; }
    public String getStatus() { return status; }
    public boolean isSimulated() { return simulated; }
    public String getOutputSummary() { return outputSummary; }
    public String getErrorCode() { return errorCode; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
