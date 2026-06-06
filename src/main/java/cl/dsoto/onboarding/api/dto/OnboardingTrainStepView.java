package cl.dsoto.onboarding.api.dto;

public record OnboardingTrainStepView(
        OnboardingTrainStep key,
        String label,
        OnboardingTrainStepStatus status
) {
}
