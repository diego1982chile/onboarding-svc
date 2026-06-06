package cl.dsoto.onboarding.api.dto;

public record RegistrationStatusResponse(
        boolean confirmed,
        OnboardingTrainView train
) {

    public RegistrationStatusResponse(OnboardingTrainView train) {
        this(train.currentStep() != OnboardingTrainStep.REGISTRATION, train);
    }
}
