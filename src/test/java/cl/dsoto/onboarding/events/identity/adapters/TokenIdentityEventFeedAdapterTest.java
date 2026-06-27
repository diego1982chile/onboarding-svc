package cl.dsoto.onboarding.events.identity.adapters;

import cl.dsoto.onboarding.clients.TokenIdentityEventFeedRestClient;
import cl.dsoto.onboarding.clients.TokenServiceAccessTokenProvider;
import cl.dsoto.onboarding.events.identity.IdentityEventFeedPage;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenIdentityEventFeedAdapterTest {

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

        TokenIdentityEventFeedAdapter client = new TokenIdentityEventFeedAdapter(restClient, accessTokenProvider);

        assertThat(client.getIdentityEvents(10L, 100), is(page));
        verify(accessTokenProvider).invalidate();
        verify(restClient, times(1)).getIdentityEvents("Bearer expired-token", 10L, 100);
        verify(restClient, times(1)).getIdentityEvents("Bearer fresh-token", 10L, 100);
    }

    @Test
    void shouldNotRetryWhenFeedFailsWithNonUnauthorizedError() {
        TokenIdentityEventFeedRestClient restClient = mock(TokenIdentityEventFeedRestClient.class);
        TokenServiceAccessTokenProvider accessTokenProvider = mock(TokenServiceAccessTokenProvider.class);
        WebApplicationException forbidden = new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN).build()
        );

        when(accessTokenProvider.authorizationHeader()).thenReturn("Bearer token");
        when(restClient.getIdentityEvents("Bearer token", 10L, 100)).thenThrow(forbidden);

        TokenIdentityEventFeedAdapter client = new TokenIdentityEventFeedAdapter(restClient, accessTokenProvider);

        assertThat(assertThrows(WebApplicationException.class, () -> client.getIdentityEvents(10L, 100)), is(forbidden));
        verify(accessTokenProvider, never()).invalidate();
        verify(restClient, times(1)).getIdentityEvents("Bearer token", 10L, 100);
    }

    @Test
    void shouldRetryOnlyOnceWhenFreshTokenIsAlsoRejected() {
        TokenIdentityEventFeedRestClient restClient = mock(TokenIdentityEventFeedRestClient.class);
        TokenServiceAccessTokenProvider accessTokenProvider = mock(TokenServiceAccessTokenProvider.class);
        WebApplicationException firstUnauthorized = new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED).build()
        );
        WebApplicationException secondUnauthorized = new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED).build()
        );

        when(accessTokenProvider.authorizationHeader())
                .thenReturn("Bearer expired-token", "Bearer rejected-fresh-token");
        when(restClient.getIdentityEvents("Bearer expired-token", 10L, 100))
                .thenThrow(firstUnauthorized);
        when(restClient.getIdentityEvents("Bearer rejected-fresh-token", 10L, 100))
                .thenThrow(secondUnauthorized);

        TokenIdentityEventFeedAdapter client = new TokenIdentityEventFeedAdapter(restClient, accessTokenProvider);

        assertThat(assertThrows(WebApplicationException.class, () -> client.getIdentityEvents(10L, 100)),
                is(secondUnauthorized));
        verify(accessTokenProvider, times(1)).invalidate();
        verify(restClient, times(1)).getIdentityEvents("Bearer expired-token", 10L, 100);
        verify(restClient, times(1)).getIdentityEvents("Bearer rejected-fresh-token", 10L, 100);
    }
}
