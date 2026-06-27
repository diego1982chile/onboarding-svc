package cl.dsoto.onboarding.clients;

import cl.dsoto.onboarding.events.profile.ProfileEventFeedPage;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RegisterRestClient(configKey = "profile-events")
@Path("/api/internal/profile-events")
public interface ProfileEventFeedRestClient {

    @GET
    @Produces(APPLICATION_JSON)
    ProfileEventFeedPage getProfileEvents(
            @HeaderParam(AUTHORIZATION) String authorization,
            @QueryParam("after") Long after,
            @QueryParam("limit") Integer limit
    );
}
