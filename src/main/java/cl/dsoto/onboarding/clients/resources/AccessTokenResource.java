package cl.dsoto.onboarding.clients.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccessTokenResource(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("expires_in")
        Long expiresIn
) {
}
