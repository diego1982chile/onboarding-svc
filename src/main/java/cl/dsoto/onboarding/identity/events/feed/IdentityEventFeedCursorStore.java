package cl.dsoto.onboarding.identity.events.feed;

import cl.dsoto.onboarding.entities.IdentityEventFeedCursorEntity;
import cl.dsoto.onboarding.repositories.IdentityEventFeedCursorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class IdentityEventFeedCursorStore {

    private final IdentityEventFeedCursorRepository cursorRepository;

    public IdentityEventFeedCursorStore(IdentityEventFeedCursorRepository cursorRepository) {
        this.cursorRepository = cursorRepository;
    }

    @Transactional
    public Long currentCursor(String source) {
        return cursorRepository.findById(source)
                .map(IdentityEventFeedCursorEntity::getCursor)
                .orElse(0L);
    }

    @Transactional
    public void saveCursor(String source, Long cursor) {
        IdentityEventFeedCursorEntity entity = cursorRepository.findById(source)
                .orElseGet(() -> IdentityEventFeedCursorEntity.builder()
                        .source(source)
                        .cursor(0L)
                        .build());
        entity.setCursor(cursor);
        entity.setUpdatedAt(Instant.now());
        cursorRepository.save(entity);
    }
}
