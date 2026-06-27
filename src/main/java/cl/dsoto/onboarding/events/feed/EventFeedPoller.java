package cl.dsoto.onboarding.events.feed;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class EventFeedPoller {

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
                itemHandler.handle(item);
                cursorStore.saveCursor(source, item.cursor());
                cursor = item.cursor();
            }

            if (!page.hasMore()) {
                return;
            }
        }
    }
}
