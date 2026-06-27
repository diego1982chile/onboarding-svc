package cl.dsoto.onboarding.clients;

import cl.dsoto.onboarding.clients.resources.AccessTokenResource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceAccessTokenProviderTest {

    @Test
    void shouldReuseCachedTokenWhileItIsValid() {
        TokenAuthRestClient restClient = mock(TokenAuthRestClient.class);
        when(restClient.clientCredentials("onboarding-svc", "secret", "token.identity-events.read"))
                .thenReturn(new AccessTokenResource("token-1", "Bearer", 3600L));

        TokenServiceAccessTokenProvider provider = new TokenServiceAccessTokenProvider(restClient);
        provider.clientId = "onboarding-svc";
        provider.clientSecret = "secret";
        provider.scope = "token.identity-events.read";
        provider.refreshSkewSeconds = 60L;

        assertThat(provider.authorizationHeader(), is("Bearer token-1"));
        assertThat(provider.authorizationHeader(), is("Bearer token-1"));

        verify(restClient, times(1))
                .clientCredentials("onboarding-svc", "secret", "token.identity-events.read");
    }

    @Test
    void shouldRequestNewTokenAfterInvalidation() {
        TokenAuthRestClient restClient = mock(TokenAuthRestClient.class);
        when(restClient.clientCredentials("onboarding-svc", "secret", "token.identity-events.read"))
                .thenReturn(
                        new AccessTokenResource("token-1", "Bearer", 3600L),
                        new AccessTokenResource("token-2", "Bearer", 3600L)
                );

        TokenServiceAccessTokenProvider provider = new TokenServiceAccessTokenProvider(restClient);
        provider.clientId = "onboarding-svc";
        provider.clientSecret = "secret";
        provider.scope = "token.identity-events.read";
        provider.refreshSkewSeconds = 60L;

        assertThat(provider.authorizationHeader(), is("Bearer token-1"));
        provider.invalidate();
        assertThat(provider.authorizationHeader(), is("Bearer token-2"));
    }

    @Test
    void shouldRefreshTokenWhenItIsInsideRefreshSkew() {
        TokenAuthRestClient restClient = mock(TokenAuthRestClient.class);
        when(restClient.clientCredentials("onboarding-svc", "secret", "token.identity-events.read"))
                .thenReturn(
                        new AccessTokenResource("token-1", "Bearer", 30L),
                        new AccessTokenResource("token-2", "Bearer", 3600L)
                );

        TokenServiceAccessTokenProvider provider = new TokenServiceAccessTokenProvider(restClient);
        provider.clientId = "onboarding-svc";
        provider.clientSecret = "secret";
        provider.scope = "token.identity-events.read";
        provider.refreshSkewSeconds = 60L;

        assertThat(provider.authorizationHeader(), is("Bearer token-1"));
        assertThat(provider.authorizationHeader(), is("Bearer token-2"));

        verify(restClient, times(2))
                .clientCredentials("onboarding-svc", "secret", "token.identity-events.read");
    }

    @Test
    void shouldCacheTokensSeparatelyByScope() {
        TokenAuthRestClient restClient = mock(TokenAuthRestClient.class);
        when(restClient.clientCredentials("onboarding-svc", "secret", "token.identity-events.read"))
                .thenReturn(new AccessTokenResource("identity-token", "Bearer", 3600L));
        when(restClient.clientCredentials("onboarding-svc", "secret", "profile.profile-events.read"))
                .thenReturn(new AccessTokenResource("profile-token", "Bearer", 3600L));

        TokenServiceAccessTokenProvider provider = new TokenServiceAccessTokenProvider(restClient);
        provider.clientId = "onboarding-svc";
        provider.clientSecret = "secret";
        provider.scope = "token.identity-events.read";
        provider.refreshSkewSeconds = 60L;

        assertThat(provider.authorizationHeader(), is("Bearer identity-token"));
        assertThat(provider.authorizationHeader("profile.profile-events.read"), is("Bearer profile-token"));
        assertThat(provider.authorizationHeader(), is("Bearer identity-token"));
        assertThat(provider.authorizationHeader("profile.profile-events.read"), is("Bearer profile-token"));

        verify(restClient, times(1))
                .clientCredentials("onboarding-svc", "secret", "token.identity-events.read");
        verify(restClient, times(1))
                .clientCredentials("onboarding-svc", "secret", "profile.profile-events.read");
    }
}
