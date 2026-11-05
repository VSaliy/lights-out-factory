package io.lightsoutfactory.execution;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRunRepository extends JpaRepository<ExecutionRun, UUID> {
    Optional<ExecutionRun> findFirstByAgentTask_Plan_IdOrderByStartedAtDesc(UUID planId);
}
