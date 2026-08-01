package cl.dsoto.onboarding.events.feed;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class EventFeedPoller {

    private static final Logger LOGGER = Logger.getLogger(EventFeedPoller.class);

    private final EventFeedCursorStore cursorStore;

    public EventFeedPoller(EventFeedCursorStore cursorStore) {
        this.cursorStore = cursorStore;
    }

    public <T extends EventFeedItem> void pollAvailablePages(
            String source,
            int limit,
            EventFeedClient<T> feedClient,
            EventFeedItemHandler<T> itemHandler
    ) {
        Long cursor = cursorStore.currentCursor(source);

        while (true) {
            EventFeedPage<T> page = feedClient.getEvents(cursor, limit);
            List<T> items = page == null || page.items() == null
                    ? List.of()
                    : page.items();

            if (items.isEmpty()) {
                return;
            }

            for (T item : items) {
                try {
                    itemHandler.handle(item);
                } catch (Exception exception) {
                    LOGGER.warnf(
                            "Skipping event feed item from %s at cursor %s: %s: %s",
                            source,
                            item.cursor(),
                            exception.getClass().getSimpleName(),
                            exception.getMessage()
                    );
                    LOGGER.debug("Event feed item handling failure details", exception);
                }
                cursorStore.saveCursor(source, item.cursor());
                cursor = item.cursor();
            }

            if (!page.hasMore()) {
                return;
            }
        }
    }
}
