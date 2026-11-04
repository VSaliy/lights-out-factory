package io.lightsoutfactory.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "agent_tasks")
public class AgentTask {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private ExecutionPlan plan;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String input;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    protected AgentTask() {
    }

    public AgentTask(ExecutionPlan plan, String title, String input) {
        this.id = UUID.randomUUID();
        this.plan = plan;
        this.title = title;
        this.input = input;
        this.status = "PENDING";
    }

    public void start() { this.status = "RUNNING"; this.attemptCount++; }
    public void finish(boolean success) { this.status = success ? "SUCCEEDED" : "FAILED"; }
    public UUID getId() { return id; }
    public ExecutionPlan getPlan() { return plan; }
    public String getTitle() { return title; }
    public String getInput() { return input; }
    public String getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
}
