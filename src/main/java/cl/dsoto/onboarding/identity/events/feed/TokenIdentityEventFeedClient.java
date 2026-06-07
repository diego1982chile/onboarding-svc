package cl.dsoto.onboarding.identity.events.feed;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class TokenIdentityEventFeedClient {

    private final TokenIdentityEventFeedRestClient feedRestClient;
    private final TokenServiceAccessTokenProvider accessTokenProvider;

    public TokenIdentityEventFeedClient(
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
}
