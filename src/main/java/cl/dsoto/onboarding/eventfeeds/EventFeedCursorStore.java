package cl.dsoto.onboarding.eventfeeds;

import cl.dsoto.onboarding.entities.EventFeedCursorEntity;
import cl.dsoto.onboarding.repositories.EventFeedCursorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class EventFeedCursorStore {

    private final EventFeedCursorRepository cursorRepository;

    public EventFeedCursorStore(EventFeedCursorRepository cursorRepository) {
        this.cursorRepository = cursorRepository;
    }

    @Transactional
    public Long currentCursor(String source) {
        return cursorRepository.findById(source)
                .map(EventFeedCursorEntity::getCursor)
                .orElse(0L);
    }

    @Transactional
    public void saveCursor(String source, Long cursor) {
        EventFeedCursorEntity entity = cursorRepository.findById(source)
                .orElseGet(() -> EventFeedCursorEntity.builder()
                        .source(source)
                        .cursor(0L)
                        .build());
        entity.setCursor(cursor);
        entity.setUpdatedAt(Instant.now());
        cursorRepository.save(entity);
    }
}
