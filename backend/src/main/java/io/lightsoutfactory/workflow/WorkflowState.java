package io.lightsoutfactory.workflow;

public enum WorkflowState {
    CREATED,
    AWAITING_APPROVAL,
    APPROVED,
    EXECUTING,
    VERIFYING,
    REVIEW_REQUIRED,
    COMPLETED,
    FAILED
}
