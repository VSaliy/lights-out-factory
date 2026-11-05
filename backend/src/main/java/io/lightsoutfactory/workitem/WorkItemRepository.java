package io.lightsoutfactory.workitem;

import java.util.List;
import java.util.UUID;
import io.lightsoutfactory.workflow.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkItemRepository extends JpaRepository<WorkItem, UUID> {
    List<WorkItem> findAllByOrderByCreatedAtDesc();
    long countByStatusIn(List<WorkflowState> statuses);
}
