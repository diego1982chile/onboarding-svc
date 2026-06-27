package cl.dsoto.onboarding.events.identity.adapters;

import cl.dsoto.onboarding.clients.TokenIdentityEventFeedRestClient;
import cl.dsoto.onboarding.clients.TokenServiceAccessTokenProvider;
import cl.dsoto.onboarding.events.feed.EventFeedClient;
import cl.dsoto.onboarding.events.feed.EventFeedPage;
import cl.dsoto.onboarding.events.identity.IdentityEventFeedItem;
import cl.dsoto.onboarding.events.identity.IdentityEventFeedPage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class TokenIdentityEventFeedAdapter implements EventFeedClient<IdentityEventFeedItem> {

    private final TokenIdentityEventFeedRestClient feedRestClient;
    private final TokenServiceAccessTokenProvider accessTokenProvider;

    public TokenIdentityEventFeedAdapter(
            @RestClient TokenIdentityEventFeedRestClient feedRestClient,
            TokenServiceAccessTokenProvider accessTokenProvider
    ) {
        this.feedRestClient = feedRestClient;
        this.accessTokenProvider = accessTokenProvider;
    }

    public IdentityEventFeedPage getIdentityEvents(Long after, Integer limit) {
        try {
            return feedRestClient.getIdentityEvents(accessTokenProvider.authorizationHeader(), after, limit);
        } catch (WebApplicationException exception) {
            if (exception.getResponse() == null || exception.getResponse().getStatus() != 401) {
                throw exception;
            }
            accessTokenProvider.invalidate();
            return feedRestClient.getIdentityEvents(accessTokenProvider.authorizationHeader(), after, limit);
        }
    }

    @Override
    public EventFeedPage<IdentityEventFeedItem> getEvents(Long after, Integer limit) {
        return getIdentityEvents(after, limit);
    }
}
