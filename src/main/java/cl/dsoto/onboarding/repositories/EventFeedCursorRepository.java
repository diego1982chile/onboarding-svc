package cl.dsoto.onboarding.repositories;

import cl.dsoto.onboarding.entities.EventFeedCursorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventFeedCursorRepository extends JpaRepository<EventFeedCursorEntity, String> {
}
