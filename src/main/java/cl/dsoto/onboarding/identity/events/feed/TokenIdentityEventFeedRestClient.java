package cl.dsoto.onboarding.identity.events.feed;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RegisterRestClient(configKey = "token-identity-events")
@Path("/api/internal/identity-events")
public interface TokenIdentityEventFeedRestClient {

    @GET
    @Produces(APPLICATION_JSON)
    IdentityEventFeedPage getIdentityEvents(
            @HeaderParam(AUTHORIZATION) String authorization,
            @QueryParam("after") Long after,
            @QueryParam("limit") Integer limit
    );
}
