package cl.dsoto.onboarding.events.identity;

import cl.dsoto.onboarding.events.feed.EventFeedPoller;
import cl.dsoto.onboarding.events.identity.adapters.TokenIdentityEventFeedAdapter;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class IdentityEventFeedPoller {

    private static final Logger LOGGER = Logger.getLogger(IdentityEventFeedPoller.class);

    private final EventFeedPoller eventFeedPoller;
    private final TokenIdentityEventFeedAdapter feedClient;
    private final IdentityEventHandler identityEventHandler;

    @ConfigProperty(name = "identity.events.feed.enabled")
    boolean enabled;

    @ConfigProperty(name = "identity.events.feed.limit")
    int limit;

    @ConfigProperty(name = "identity.events.feed.source")
    String source;

    public IdentityEventFeedPoller(
            EventFeedPoller eventFeedPoller,
            TokenIdentityEventFeedAdapter feedClient,
            IdentityEventHandler identityEventHandler
    ) {
        this.eventFeedPoller = eventFeedPoller;
        this.feedClient = feedClient;
        this.identityEventHandler = identityEventHandler;
    }

    @Scheduled(every = "{identity.events.feed.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        if (!enabled) {
            return;
        }

        try {
            pollAvailablePages();
        } catch (Exception exception) {
            LOGGER.warnf("Unable to poll identity events from token-svc: %s: %s",
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            LOGGER.debug("Identity event feed polling failure details", exception);
        }
    }

    void pollAvailablePages() {
        eventFeedPoller.pollAvailablePages(source, limit, feedClient, identityEventHandler::handle);
    }
}
