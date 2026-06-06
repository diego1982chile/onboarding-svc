package cl.dsoto.onboarding.services;

import cl.dsoto.onboarding.resources.dto.OnboardingTrainView;

import java.util.Optional;

public interface OnboardingTrainService {

    OnboardingTrainView getPublicTrain();

    OnboardingTrainView getAuthenticatedTrain(String username);

    Optional<OnboardingTrainView> getRegistrationStatus(String registrationId);
}
