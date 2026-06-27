package cl.dsoto.onboarding.webservice.impl;

import cl.dsoto.onboarding.services.OnboardingStartService;
import cl.dsoto.onboarding.services.OnboardingTrainService;
import cl.dsoto.onboarding.webservice.OnboardingWebService;
import cl.dsoto.onboarding.webservice.resources.OnboardingStartRequestResource;
import cl.dsoto.onboarding.webservice.resources.OnboardingTrainResource;
import cl.dsoto.onboarding.webservice.resources.RegistrationStatusResource;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/onboarding")
@Produces(MediaType.APPLICATION_JSON)
public class DefaultOnboardingWebService implements OnboardingWebService {

    private final OnboardingStartService startService;
    private final OnboardingTrainService trainService;

    @Inject
    JsonWebToken jwt;

    public DefaultOnboardingWebService(
            OnboardingStartService startService,
            OnboardingTrainService trainService
    ) {
        this.startService = startService;
        this.trainService = trainService;
    }

    @POST
    @Path("/start")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public Response start(@Valid OnboardingStartRequestResource request) {
        return Response.ok(startService.start(request.getEmail())).build();
    }

    @GET
    @Path("/train")
    @PermitAll
    @Override
    public OnboardingTrainResource getPublicTrain() {
        return trainService.getPublicTrain();
    }

    @GET
    @Path("/registrations/{registrationId}/status")
    @PermitAll
    @Override
    public Response getRegistrationStatus(@PathParam("registrationId") @NotBlank String registrationId) {
        return trainService.getRegistrationStatus(registrationId)
                .map(RegistrationStatusResource::new)
                .map(status -> Response.ok(status).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/me/train")
    @RolesAllowed({"USER", "ADMIN"})
    @Override
    public Response getMyTrain() {
        OnboardingTrainResource train = trainService.getAuthenticatedTrain(jwt.getSubject());
        if (train == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(train).build();
    }
}
