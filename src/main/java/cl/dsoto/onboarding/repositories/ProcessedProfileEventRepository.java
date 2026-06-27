package cl.dsoto.onboarding.repositories;

import cl.dsoto.onboarding.entities.ProcessedProfileEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedProfileEventRepository extends JpaRepository<ProcessedProfileEventEntity, String> {

    boolean existsByEventId(String eventId);
}
