package cl.dsoto.onboarding.repositories;

import cl.dsoto.onboarding.entities.IdentityEventFeedCursorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityEventFeedCursorRepository extends JpaRepository<IdentityEventFeedCursorEntity, String> {
}
