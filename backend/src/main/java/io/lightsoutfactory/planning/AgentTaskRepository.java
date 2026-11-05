package io.lightsoutfactory.planning;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentTaskRepository extends JpaRepository<AgentTask, UUID> {
    Optional<AgentTask> findFirstByPlan_Id(UUID planId);
}
