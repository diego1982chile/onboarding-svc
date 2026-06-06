package cl.dsoto.onboarding.services;

import cl.dsoto.onboarding.webservice.resources.OnboardingTrainResource;

import java.util.Optional;

public interface OnboardingTrainService {

    OnboardingTrainResource getPublicTrain();

    OnboardingTrainResource getAuthenticatedTrain(String username);

    Optional<OnboardingTrainResource> getRegistrationStatus(String registrationId);
}
