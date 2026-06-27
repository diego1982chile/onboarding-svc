package cl.dsoto.onboarding.webservice;

import cl.dsoto.onboarding.webservice.resources.OnboardingTrainResource;
import cl.dsoto.onboarding.webservice.resources.OnboardingStartRequestResource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.core.Response;

public interface OnboardingWebService {

    Response start(@Valid OnboardingStartRequestResource request);

    OnboardingTrainResource getPublicTrain();

    Response getRegistrationStatus(@NotBlank String registrationId);

    Response getMyTrain();
}
