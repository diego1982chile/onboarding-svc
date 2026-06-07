package cl.dsoto.onboarding.clients;

import cl.dsoto.onboarding.clients.resources.AccessTokenResource;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RegisterRestClient(configKey = "token-auth")
@Path("/api/auth")
public interface TokenAuthRestClient {

    @POST
    @Path("/client-credentials")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    AccessTokenResource clientCredentials(
            @RestForm("client_id") String clientId,
            @RestForm("client_secret") String clientSecret,
            @RestForm("scope") String scope
    );
}
