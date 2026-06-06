package cl.dsoto.onboarding.repositories;

import cl.dsoto.onboarding.entities.ProcessedIdentityEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedIdentityEventRepository extends JpaRepository<ProcessedIdentityEventEntity, String> {

    boolean existsByEventId(String eventId);
}
