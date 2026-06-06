package cl.dsoto.onboarding.resources;

import cl.dsoto.onboarding.resources.dto.RegistrationStatusResponse;
import cl.dsoto.onboarding.resources.dto.OnboardingTrainView;
import cl.dsoto.onboarding.services.OnboardingTrainService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/onboarding")
@Produces(MediaType.APPLICATION_JSON)
public class OnboardingResource {

    private final OnboardingTrainService trainService;

    @Inject
    JsonWebToken jwt;

    public OnboardingResource(OnboardingTrainService trainService) {
        this.trainService = trainService;
    }

    @GET
    @Path("/public/train")
    @PermitAll
    public OnboardingTrainView getPublicTrain() {
        return trainService.getPublicTrain();
    }

    @GET
    @Path("/public/{registrationId}/status")
    @PermitAll
    public Response getRegistrationStatus(@PathParam("registrationId") @NotBlank String registrationId) {
        return trainService.getRegistrationStatus(registrationId)
                .map(RegistrationStatusResponse::new)
                .map(status -> Response.ok(status).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/me/train")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getMyTrain() {
        OnboardingTrainView train = trainService.getAuthenticatedTrain(jwt.getSubject());
        if (train == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(train).build();
    }
}
