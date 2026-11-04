package io.lightsoutfactory.workitem;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkItemRepository extends JpaRepository<WorkItem, UUID> {
    List<WorkItem> findAllByOrderByCreatedAtDesc();
}
