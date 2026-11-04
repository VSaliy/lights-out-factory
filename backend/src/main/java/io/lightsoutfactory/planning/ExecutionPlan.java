package io.lightsoutfactory.planning;

import io.lightsoutfactory.workitem.WorkItem;
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
@Table(name = "execution_plans")
public class ExecutionPlan {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private WorkItem workItem;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "approved_by", length = 80)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @jakarta.persistence.Version
    private long version;

    protected ExecutionPlan() {
    }

    public ExecutionPlan(WorkItem workItem, String summary) {
        this.id = UUID.randomUUID();
        this.workItem = workItem;
        this.summary = summary;
        this.status = "AWAITING_APPROVAL";
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public void approve(String actor) {
        this.status = "APPROVED";
        this.approvedBy = actor;
        this.approvedAt = Instant.now();
    }

    public void markExecuting() { this.status = "EXECUTING"; }
    public void markFinished(boolean success) { this.status = success ? "REVIEW_REQUIRED" : "FAILED"; }
    public UUID getId() { return id; }
    public WorkItem getWorkItem() { return workItem; }
    public String getSummary() { return summary; }
    public String getStatus() { return status; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
