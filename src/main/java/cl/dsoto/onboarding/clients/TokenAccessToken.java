package cl.dsoto.onboarding.clients;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenAccessToken(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("expires_in")
        Long expiresIn
) {
}
