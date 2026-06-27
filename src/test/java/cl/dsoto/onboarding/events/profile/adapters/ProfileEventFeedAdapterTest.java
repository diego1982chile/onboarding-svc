package cl.dsoto.onboarding.events.profile.adapters;

import cl.dsoto.onboarding.clients.ProfileEventFeedRestClient;
import cl.dsoto.onboarding.clients.TokenServiceAccessTokenProvider;
import cl.dsoto.onboarding.events.profile.ProfileEventFeedPage;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileEventFeedAdapterTest {

    @Test
    void shouldRenewTokenAndRetryOnceWhenFeedRejectsBearerToken() {
        ProfileEventFeedRestClient restClient = mock(ProfileEventFeedRestClient.class);
        TokenServiceAccessTokenProvider accessTokenProvider = mock(TokenServiceAccessTokenProvider.class);
        ProfileEventFeedPage page = new ProfileEventFeedPage(List.of(), 0L, false);

        when(accessTokenProvider.authorizationHeader("profile.profile-events.read"))
                .thenReturn("Bearer expired-token", "Bearer fresh-token");
        when(restClient.getProfileEvents("Bearer expired-token", 10L, 100))
                .thenThrow(new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build()));
        when(restClient.getProfileEvents("Bearer fresh-token", 10L, 100))
                .thenReturn(page);

        ProfileEventFeedAdapter client = new ProfileEventFeedAdapter(restClient, accessTokenProvider);
        client.scope = "profile.profile-events.read";

        assertThat(client.getProfileEvents(10L, 100), is(page));
        verify(accessTokenProvider).invalidate("profile.profile-events.read");
        verify(restClient, times(1)).getProfileEvents("Bearer expired-token", 10L, 100);
        verify(restClient, times(1)).getProfileEvents("Bearer fresh-token", 10L, 100);
    }

    @Test
    void shouldNotRetryWhenFeedFailsWithNonUnauthorizedError() {
        ProfileEventFeedRestClient restClient = mock(ProfileEventFeedRestClient.class);
        TokenServiceAccessTokenProvider accessTokenProvider = mock(TokenServiceAccessTokenProvider.class);
        WebApplicationException forbidden = new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN).build()
        );

        when(accessTokenProvider.authorizationHeader("profile.profile-events.read")).thenReturn("Bearer token");
        when(restClient.getProfileEvents("Bearer token", 10L, 100)).thenThrow(forbidden);

        ProfileEventFeedAdapter client = new ProfileEventFeedAdapter(restClient, accessTokenProvider);
        client.scope = "profile.profile-events.read";

        assertThat(assertThrows(WebApplicationException.class, () -> client.getProfileEvents(10L, 100)), is(forbidden));
        verify(accessTokenProvider, never()).invalidate("profile.profile-events.read");
        verify(restClient, times(1)).getProfileEvents("Bearer token", 10L, 100);
    }
}
