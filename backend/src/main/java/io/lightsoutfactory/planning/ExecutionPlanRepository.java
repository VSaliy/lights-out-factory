package io.lightsoutfactory.planning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionPlanRepository extends JpaRepository<ExecutionPlan, UUID> {
    List<ExecutionPlan> findByWorkItem_IdOrderByCreatedAtDesc(UUID workItemId);
    Optional<ExecutionPlan> findFirstByWorkItem_IdOrderByCreatedAtDesc(UUID workItemId);
}
