package cl.dsoto.onboarding.clients;

import cl.dsoto.onboarding.clients.resources.AccessTokenResource;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;

@ApplicationScoped
public class TokenServiceAccessTokenProvider {

    private final TokenAuthRestClient tokenAuthRestClient;

    @ConfigProperty(name = "token.service.client-id")
    String clientId;

    @ConfigProperty(name = "token.service.client-secret")
    String clientSecret;

    @ConfigProperty(name = "token.service.identity-events.scope")
    String scope;

    @ConfigProperty(name = "token.service.access-token-refresh-skew-seconds")
    long refreshSkewSeconds;

    private volatile CachedAccessToken cachedAccessToken;

    public TokenServiceAccessTokenProvider(@RestClient TokenAuthRestClient tokenAuthRestClient) {
        this.tokenAuthRestClient = tokenAuthRestClient;
    }

    public String authorizationHeader() {
        return "Bearer " + currentToken();
    }

    public void invalidate() {
        cachedAccessToken = null;
    }

    private String currentToken() {
        CachedAccessToken token = cachedAccessToken;
        if (token != null && token.isValid(refreshSkewSeconds)) {
            return token.value();
        }

        synchronized (this) {
            token = cachedAccessToken;
            if (token != null && token.isValid(refreshSkewSeconds)) {
                return token.value();
            }

            AccessTokenResource response = tokenAuthRestClient.clientCredentials(clientId, clientSecret, scope);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IllegalStateException("token-svc did not return an access token");
            }

            long expiresIn = response.expiresIn() == null ? 0L : response.expiresIn();
            cachedAccessToken = new CachedAccessToken(
                    response.accessToken(),
                    Instant.now().plusSeconds(expiresIn)
            );
            return cachedAccessToken.value();
        }
    }

    private record CachedAccessToken(String value, Instant expiresAt) {

        boolean isValid(long refreshSkewSeconds) {
            return Instant.now().plusSeconds(refreshSkewSeconds).isBefore(expiresAt);
        }
    }
}
