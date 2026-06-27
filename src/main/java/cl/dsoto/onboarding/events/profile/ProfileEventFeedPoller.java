package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.events.feed.EventFeedPoller;
import cl.dsoto.onboarding.events.profile.adapters.ProfileEventFeedAdapter;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProfileEventFeedPoller {

    private static final Logger LOGGER = Logger.getLogger(ProfileEventFeedPoller.class);

    private final EventFeedPoller eventFeedPoller;
    private final ProfileEventFeedAdapter feedClient;
    private final ProfileEventHandler profileEventHandler;

    @ConfigProperty(name = "profile.events.feed.enabled")
    boolean enabled;

    @ConfigProperty(name = "profile.events.feed.limit")
    int limit;

    @ConfigProperty(name = "profile.events.feed.source")
    String source;

    public ProfileEventFeedPoller(
            EventFeedPoller eventFeedPoller,
            ProfileEventFeedAdapter feedClient,
            ProfileEventHandler profileEventHandler
    ) {
        this.eventFeedPoller = eventFeedPoller;
        this.feedClient = feedClient;
        this.profileEventHandler = profileEventHandler;
    }

    @Scheduled(every = "{profile.events.feed.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        if (!enabled) {
            return;
        }

        try {
            pollAvailablePages();
        } catch (Exception exception) {
            LOGGER.warnf("Unable to poll profile events from profile-svc: %s: %s",
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            LOGGER.debug("Profile event feed polling failure details", exception);
        }
    }

    void pollAvailablePages() {
        eventFeedPoller.pollAvailablePages(source, limit, feedClient, profileEventHandler::handle);
    }
}
