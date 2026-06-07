package cl.dsoto.onboarding.eventfeeds;

import cl.dsoto.onboarding.clients.facade.TokenIdentityEventFeedClient;
import cl.dsoto.onboarding.identity.events.IdentityEventFeedItem;
import cl.dsoto.onboarding.identity.events.IdentityEventFeedPage;
import cl.dsoto.onboarding.identity.events.IdentityEventHandler;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class EventFeedPoller {

    private static final Logger LOGGER = Logger.getLogger(EventFeedPoller.class);

    private final TokenIdentityEventFeedClient feedClient;
    private final IdentityEventHandler identityEventHandler;
    private final EventFeedCursorStore cursorStore;

    @ConfigProperty(name = "identity.events.feed.enabled")
    boolean enabled;

    @ConfigProperty(name = "identity.events.feed.limit")
    int limit;

    @ConfigProperty(name = "identity.events.feed.source")
    String source;

    public EventFeedPoller(
            TokenIdentityEventFeedClient feedClient,
            IdentityEventHandler identityEventHandler,
            EventFeedCursorStore cursorStore
    ) {
        this.feedClient = feedClient;
        this.identityEventHandler = identityEventHandler;
        this.cursorStore = cursorStore;
    }

    @Scheduled(every = "{identity.events.feed.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        if (!enabled) {
            return;
        }

        try {
            pollAvailablePages();
        } catch (Exception exception) {
            LOGGER.error("Unable to poll identity events from token-svc", exception);
        }
    }

    void pollAvailablePages() {
        Long cursor = cursorStore.currentCursor(source);

        while (true) {
            IdentityEventFeedPage page = feedClient.getIdentityEvents(cursor, limit);
            List<IdentityEventFeedItem> items = page == null || page.items() == null
                    ? List.of()
                    : page.items();

            if (items.isEmpty()) {
                return;
            }

            for (IdentityEventFeedItem item : items) {
                identityEventHandler.handle(item.toIdentityEvent());
                cursorStore.saveCursor(source, item.cursor());
                cursor = item.cursor();
            }

            if (!page.hasMore()) {
                return;
            }
        }
    }
}
