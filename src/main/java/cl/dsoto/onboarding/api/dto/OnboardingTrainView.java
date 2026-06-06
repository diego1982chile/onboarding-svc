package cl.dsoto.onboarding.api.dto;

import cl.dsoto.onboarding.domain.OnboardingState;

import java.util.List;

public record OnboardingTrainView(
        String username,
        OnboardingState currentState,
        OnboardingTrainStep currentStep,
        List<OnboardingTrainStepView> steps
) {
}
