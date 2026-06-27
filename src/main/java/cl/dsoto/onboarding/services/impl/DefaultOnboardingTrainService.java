package cl.dsoto.onboarding.services.impl;

import cl.dsoto.onboarding.webservice.resources.OnboardingTrainStep;
import cl.dsoto.onboarding.webservice.resources.OnboardingTrainStepStatus;
import cl.dsoto.onboarding.webservice.resources.OnboardingTrainStepResource;
import cl.dsoto.onboarding.webservice.resources.OnboardingTrainResource;
import cl.dsoto.onboarding.model.OnboardingState;
import cl.dsoto.onboarding.entities.OnboardingProcessEntity;
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
    public OnboardingTrainResource getPublicTrain() {
        return trainView(null, null, OnboardingTrainStep.REGISTRATION);
    }

    @Override
    public OnboardingTrainResource getAuthenticatedTrain(String username) {
        OnboardingState currentState = onboardingEngine.getCurrentState(username);
        if (currentState == null) {
            return null;
        }

        return trainView(username, currentState, currentStepFor(currentState));
    }

    @Override
    public OnboardingTrainResource getPublicTrainForUsername(String username) {
        if (username == null || username.isBlank()) {
            return getPublicTrain();
        }

        return onboardingProcessRepository.findByUsernameIgnoreCase(username)
                .map(this::trainView)
                .orElseGet(this::getPublicTrain);
    }

    @Override
    public Optional<OnboardingTrainResource> getRegistrationStatus(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return Optional.empty();
        }

        return onboardingProcessRepository.findByRegistrationId(registrationId)
                .map(this::trainView);
    }

    private OnboardingTrainResource trainView(OnboardingProcessEntity process) {
        OnboardingState currentState = process.getCurrentState();
        return trainView(null, currentState, currentStepFor(currentState));
    }

    private OnboardingTrainResource trainView(
            String username,
            OnboardingState currentState,
            OnboardingTrainStep currentStep
    ) {
        return new OnboardingTrainResource(
                username,
                currentState,
                currentStep,
                List.of(
                        step(OnboardingTrainStep.REGISTRATION, "Registro", statusFor(OnboardingTrainStep.REGISTRATION, currentStep, currentState)),
                        step(OnboardingTrainStep.EMAIL_VERIFICATION, "Verifica tu correo", statusFor(OnboardingTrainStep.EMAIL_VERIFICATION, currentStep, currentState)),
                        step(OnboardingTrainStep.PROFILE_CREATION, "Crea tu perfil", statusFor(OnboardingTrainStep.PROFILE_CREATION, currentStep, currentState))
                )
        );
    }

    private OnboardingTrainStep currentStepFor(OnboardingState currentState) {
        return switch (currentState) {
            case REGISTERED -> OnboardingTrainStep.EMAIL_VERIFICATION;
            case EMAIL_VERIFIED, PROFILE_CREATED -> OnboardingTrainStep.PROFILE_CREATION;
        };
    }

    private OnboardingTrainStepStatus statusFor(
            OnboardingTrainStep step,
            OnboardingTrainStep currentStep,
            OnboardingState currentState
    ) {
        if (currentState == OnboardingState.PROFILE_CREATED) {
            return OnboardingTrainStepStatus.COMPLETED;
        }

        if (step == currentStep) {
            return OnboardingTrainStepStatus.CURRENT;
        }

        return step.ordinal() < currentStep.ordinal()
                ? OnboardingTrainStepStatus.COMPLETED
                : OnboardingTrainStepStatus.PENDING;
    }

    private OnboardingTrainStepResource step(
            OnboardingTrainStep step,
            String label,
            OnboardingTrainStepStatus status
    ) {
        return new OnboardingTrainStepResource(step, label, status);
    }
}
