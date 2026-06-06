package cl.dsoto.onboarding.webservice;

import cl.dsoto.onboarding.webservice.resources.OnboardingTrainResource;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.core.Response;

public interface OnboardingWebService {

    OnboardingTrainResource getPublicTrain();

    Response getRegistrationStatus(@NotBlank String registrationId);

    Response getMyTrain();
}
