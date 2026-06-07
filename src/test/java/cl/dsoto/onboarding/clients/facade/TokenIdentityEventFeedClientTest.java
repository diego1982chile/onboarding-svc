package cl.dsoto.onboarding.clients.facade;

import cl.dsoto.onboarding.clients.TokenIdentityEventFeedRestClient;
import cl.dsoto.onboarding.clients.TokenServiceAccessTokenProvider;
import cl.dsoto.onboarding.identity.events.IdentityEventFeedPage;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenIdentityEventFeedClientTest {

    @Test
    void shouldRenewTokenAndRetryOnceWhenFeedRejectsBearerToken() {
        TokenIdentityEventFeedRestClient restClient = mock(TokenIdentityEventFeedRestClient.class);
        TokenServiceAccessTokenProvider accessTokenProvider = mock(TokenServiceAccessTokenProvider.class);
        IdentityEventFeedPage page = new IdentityEventFeedPage(List.of(), 0L, false);

        when(accessTokenProvider.authorizationHeader())
                .thenReturn("Bearer expired-token", "Bearer fresh-token");
        when(restClient.getIdentityEvents("Bearer expired-token", 10L, 100))
                .thenThrow(new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build()));
        when(restClient.getIdentityEvents("Bearer fresh-token", 10L, 100))
                .thenReturn(page);

        TokenIdentityEventFeedClient client = new TokenIdentityEventFeedClient(restClient, accessTokenProvider);

        assertThat(client.getIdentityEvents(10L, 100), is(page));
        verify(accessTokenProvider).invalidate();
        verify(restClient, times(1)).getIdentityEvents("Bearer expired-token", 10L, 100);
        verify(restClient, times(1)).getIdentityEvents("Bearer fresh-token", 10L, 100);
    }
}
