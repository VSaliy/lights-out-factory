package io.lightsoutfactory.verification;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityGateResultRepository extends JpaRepository<QualityGateResult, UUID> {
    Optional<QualityGateResult> findFirstByExecutionRun_Id(UUID runId);
}
