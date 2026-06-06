package cl.dsoto.onboarding.repositories;

import cl.dsoto.onboarding.entities.ProcessedIdentityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedIdentityEventRepository extends JpaRepository<ProcessedIdentityEvent, String> {

    boolean existsByEventId(String eventId);
}
