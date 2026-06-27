package cl.dsoto.onboarding.events.profile.adapters;

import cl.dsoto.onboarding.clients.ProfileEventFeedRestClient;
import cl.dsoto.onboarding.clients.TokenServiceAccessTokenProvider;
import cl.dsoto.onboarding.events.feed.EventFeedClient;
import cl.dsoto.onboarding.events.feed.EventFeedPage;
import cl.dsoto.onboarding.events.profile.ProfileEventFeedItem;
import cl.dsoto.onboarding.events.profile.ProfileEventFeedPage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ProfileEventFeedAdapter implements EventFeedClient<ProfileEventFeedItem> {

    private final ProfileEventFeedRestClient feedRestClient;
    private final TokenServiceAccessTokenProvider accessTokenProvider;

    @ConfigProperty(name = "profile.service.profile-events.scope")
    String scope;

    public ProfileEventFeedAdapter(
            @RestClient ProfileEventFeedRestClient feedRestClient,
            TokenServiceAccessTokenProvider accessTokenProvider
    ) {
        this.feedRestClient = feedRestClient;
        this.accessTokenProvider = accessTokenProvider;
    }

    public ProfileEventFeedPage getProfileEvents(Long after, Integer limit) {
        try {
            return feedRestClient.getProfileEvents(accessTokenProvider.authorizationHeader(scope), after, limit);
        } catch (WebApplicationException exception) {
            if (exception.getResponse() == null || exception.getResponse().getStatus() != 401) {
                throw exception;
            }
            accessTokenProvider.invalidate(scope);
            return feedRestClient.getProfileEvents(accessTokenProvider.authorizationHeader(scope), after, limit);
        }
    }

    @Override
    public EventFeedPage<ProfileEventFeedItem> getEvents(Long after, Integer limit) {
        return getProfileEvents(after, limit);
    }
}
