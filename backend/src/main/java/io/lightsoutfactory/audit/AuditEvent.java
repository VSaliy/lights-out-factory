package io.lightsoutfactory.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String actor;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "work_item_id", nullable = false)
    private UUID workItemId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(String actor, String action, String resourceType, UUID resourceId, UUID workItemId, UUID correlationId) {
        this.id = UUID.randomUUID();
        this.actor = actor;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.workItemId = workItemId;
        this.correlationId = correlationId;
    }

    @PrePersist
    void onCreate() { this.occurredAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public UUID getWorkItemId() { return workItemId; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
}
