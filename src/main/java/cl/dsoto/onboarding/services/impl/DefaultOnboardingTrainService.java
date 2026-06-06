package cl.dsoto.onboarding.services.impl;

import cl.dsoto.onboarding.resources.dto.OnboardingTrainStep;
import cl.dsoto.onboarding.resources.dto.OnboardingTrainStepStatus;
import cl.dsoto.onboarding.resources.dto.OnboardingTrainStepView;
import cl.dsoto.onboarding.resources.dto.OnboardingTrainView;
import cl.dsoto.onboarding.model.OnboardingState;
import cl.dsoto.onboarding.entities.OnboardingProcess;
import cl.dsoto.onboarding.repositories.OnboardingProcessRepository;
import cl.dsoto.onboarding.services.OnboardingEngine;
import cl.dsoto.onboarding.services.OnboardingTrainService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DefaultOnboardingTrainService implements OnboardingTrainService {

    private final OnboardingEngine onboardingEngine;
    private final OnboardingProcessRepository onboardingProcessRepository;

    public DefaultOnboardingTrainService(
            OnboardingEngine onboardingEngine,
            OnboardingProcessRepository onboardingProcessRepository
    ) {
        this.onboardingEngine = onboardingEngine;
        this.onboardingProcessRepository = onboardingProcessRepository;
    }

    @Override
    public OnboardingTrainView getPublicTrain() {
        return trainView(null, null, OnboardingTrainStep.REGISTRATION);
    }

    @Override
    public OnboardingTrainView getAuthenticatedTrain(String username) {
        OnboardingState currentState = onboardingEngine.getCurrentState(username);
        if (currentState == null) {
            return null;
        }

        return trainView(username, currentState, currentStepFor(currentState));
    }

    @Override
    public Optional<OnboardingTrainView> getRegistrationStatus(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return Optional.empty();
        }

        return onboardingProcessRepository.findByRegistrationId(registrationId)
                .map(this::trainView);
    }

    private OnboardingTrainView trainView(OnboardingProcess process) {
        OnboardingState currentState = process.getCurrentState();
        return trainView(null, currentState, currentStepFor(currentState));
    }

    private OnboardingTrainView trainView(
            String username,
            OnboardingState currentState,
            OnboardingTrainStep currentStep
    ) {
        return new OnboardingTrainView(
                username,
                currentState,
                currentStep,
                List.of(
                        step(OnboardingTrainStep.REGISTRATION, "Registro", statusFor(OnboardingTrainStep.REGISTRATION, currentStep, currentState)),
                        step(OnboardingTrainStep.IDENTITY_CHECK, "Comprueba tu identidad", statusFor(OnboardingTrainStep.IDENTITY_CHECK, currentStep, currentState)),
                        step(OnboardingTrainStep.PLAN_SELECTION, "Elige tu plan", statusFor(OnboardingTrainStep.PLAN_SELECTION, currentStep, currentState))
                )
        );
    }

    private OnboardingTrainStep currentStepFor(OnboardingState currentState) {
        return switch (currentState) {
            case REGISTERED -> OnboardingTrainStep.REGISTRATION;
            case EMAIL_VERIFIED -> OnboardingTrainStep.IDENTITY_CHECK;
            case KYC_APPROVED, PLAN_SELECTED, PROFILE_COMPLETED, READY_TO_PUBLISH -> OnboardingTrainStep.PLAN_SELECTION;
        };
    }

    private OnboardingTrainStepStatus statusFor(
            OnboardingTrainStep step,
            OnboardingTrainStep currentStep,
            OnboardingState currentState
    ) {
        if (currentState == OnboardingState.READY_TO_PUBLISH) {
            return OnboardingTrainStepStatus.COMPLETED;
        }

        if (step == currentStep) {
            return OnboardingTrainStepStatus.CURRENT;
        }

        return step.ordinal() < currentStep.ordinal()
                ? OnboardingTrainStepStatus.COMPLETED
                : OnboardingTrainStepStatus.PENDING;
    }

    private OnboardingTrainStepView step(
            OnboardingTrainStep step,
            String label,
            OnboardingTrainStepStatus status
    ) {
        return new OnboardingTrainStepView(step, label, status);
    }
}
