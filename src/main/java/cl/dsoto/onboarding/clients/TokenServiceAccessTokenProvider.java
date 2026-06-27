package cl.dsoto.onboarding.clients;

import cl.dsoto.onboarding.clients.resources.AccessTokenResource;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, CachedAccessToken> cachedAccessTokens = new ConcurrentHashMap<>();

    public TokenServiceAccessTokenProvider(@RestClient TokenAuthRestClient tokenAuthRestClient) {
        this.tokenAuthRestClient = tokenAuthRestClient;
    }

    public String authorizationHeader() {
        return authorizationHeader(scope);
    }

    public String authorizationHeader(String scope) {
        return "Bearer " + currentToken(scope);
    }

    public void invalidate() {
        invalidate(scope);
    }

    public void invalidate(String scope) {
        cachedAccessTokens.remove(scope);
    }

    private String currentToken(String scope) {
        CachedAccessToken token = cachedAccessTokens.get(scope);
        if (token != null && token.isValid(refreshSkewSeconds)) {
            return token.value();
        }

        synchronized (cachedAccessTokens) {
            token = cachedAccessTokens.get(scope);
            if (token != null && token.isValid(refreshSkewSeconds)) {
                return token.value();
            }

            AccessTokenResource response = tokenAuthRestClient.clientCredentials(clientId, clientSecret, scope);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IllegalStateException("token-svc did not return an access token");
            }

            long expiresIn = response.expiresIn() == null ? 0L : response.expiresIn();
            CachedAccessToken refreshedToken = new CachedAccessToken(
                    response.accessToken(),
                    Instant.now().plusSeconds(expiresIn)
            );
            cachedAccessTokens.put(scope, refreshedToken);
            return refreshedToken.value();
        }
    }

    private record CachedAccessToken(String value, Instant expiresAt) {

        boolean isValid(long refreshSkewSeconds) {
            return Instant.now().plusSeconds(refreshSkewSeconds).isBefore(expiresAt);
        }
    }
}
