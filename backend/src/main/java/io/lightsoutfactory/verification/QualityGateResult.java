package io.lightsoutfactory.verification;

import io.lightsoutfactory.execution.ExecutionRun;
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
@Table(name = "quality_gate_results")
public class QualityGateResult {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_run_id", nullable = false)
    private ExecutionRun executionRun;

    @Column(name = "gate_name", nullable = false, length = 80)
    private String gateName;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "evidence_summary", nullable = false, length = 500)
    private String evidenceSummary;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected QualityGateResult() {
    }

    public QualityGateResult(ExecutionRun run, String status, String evidence) {
        this.id = UUID.randomUUID();
        this.executionRun = run;
        this.gateName = "mock-output-valid";
        this.status = status;
        this.evidenceSummary = evidence;
    }

    @PrePersist
    void onCreate() { this.evaluatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public ExecutionRun getExecutionRun() { return executionRun; }
    public String getGateName() { return gateName; }
    public String getStatus() { return status; }
    public String getEvidenceSummary() { return evidenceSummary; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
