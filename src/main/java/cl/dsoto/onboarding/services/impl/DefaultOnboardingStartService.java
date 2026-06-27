package cl.dsoto.onboarding.services.impl;

import cl.dsoto.onboarding.entities.OnboardingProcessEntity;
import cl.dsoto.onboarding.model.OnboardingState;
import cl.dsoto.onboarding.repositories.OnboardingProcessRepository;
import cl.dsoto.onboarding.services.OnboardingStartService;
import cl.dsoto.onboarding.services.OnboardingTrainService;
import cl.dsoto.onboarding.webservice.resources.OnboardingStartAction;
import cl.dsoto.onboarding.webservice.resources.OnboardingStartResource;
import cl.dsoto.onboarding.webservice.resources.OnboardingStartState;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;

@ApplicationScoped
public class DefaultOnboardingStartService implements OnboardingStartService {

    private final OnboardingProcessRepository repository;
    private final OnboardingTrainService trainService;

    public DefaultOnboardingStartService(
            OnboardingProcessRepository repository,
            OnboardingTrainService trainService
    ) {
        this.repository = repository;
        this.trainService = trainService;
    }

    @Override
    public OnboardingStartResource start(String email) {
        String normalizedEmail = normalizeEmail(email);

        return repository.findByUsernameIgnoreCase(normalizedEmail)
                .map(process -> existingProcess(normalizedEmail, process))
                .orElseGet(() -> newProcess(normalizedEmail));
    }

    private OnboardingStartResource newProcess(String email) {
        return new OnboardingStartResource(
                email,
                null,
                OnboardingStartState.NEW,
                OnboardingStartAction.COLLECT_PASSWORD,
                trainService.getPublicTrain()
        );
    }

    private OnboardingStartResource existingProcess(String email, OnboardingProcessEntity process) {
        OnboardingState currentState = process.getCurrentState();
        return new OnboardingStartResource(
                email,
                process.getRegistrationId(),
                OnboardingStartState.from(currentState),
                nextActionFor(currentState),
                trainService.getPublicTrainForUsername(email)
        );
    }

    private OnboardingStartAction nextActionFor(OnboardingState currentState) {
        return switch (currentState) {
            case REGISTERED -> OnboardingStartAction.SHOW_EMAIL_VERIFICATION_PENDING;
            case EMAIL_VERIFIED, PROFILE_CREATED -> OnboardingStartAction.GO_TO_LOGIN;
        };
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
