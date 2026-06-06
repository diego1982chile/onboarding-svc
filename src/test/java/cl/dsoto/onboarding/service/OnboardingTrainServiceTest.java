package cl.dsoto.onboarding.service;

import cl.dsoto.onboarding.api.dto.OnboardingTrainStep;
import cl.dsoto.onboarding.api.dto.OnboardingTrainStepStatus;
import cl.dsoto.onboarding.api.dto.OnboardingTrainView;
import cl.dsoto.onboarding.domain.OnboardingEvent;
import cl.dsoto.onboarding.domain.OnboardingState;
import cl.dsoto.onboarding.persistence.OnboardingProcessRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class OnboardingTrainServiceTest {

    @Inject
    OnboardingEngine onboardingEngine;

    @Inject
    OnboardingTrainService trainService;

    @Inject
    OnboardingProcessRepository repository;

    @BeforeEach
    void cleanUp() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
    }

    @Test
    void shouldShowPublicRegistrationTrain() {
        OnboardingTrainView view = trainService.getPublicTrain();

        assertThat(view.username(), is((String) null));
        assertThat(view.currentState(), is((OnboardingState) null));
        assertThat(view.currentStep(), is(OnboardingTrainStep.REGISTRATION));
        assertThat(view.steps().get(0).status(), is(OnboardingTrainStepStatus.CURRENT));
        assertThat(view.steps().get(1).status(), is(OnboardingTrainStepStatus.PENDING));
        assertThat(view.steps().get(2).status(), is(OnboardingTrainStepStatus.PENDING));
    }

    @Test
    void shouldShowRegistrationAsCurrentWhenUserIsRegistered() {
        String username = "registered.user@example.com";

        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));

        OnboardingTrainView view = trainService.getAuthenticatedTrain(username);

        assertThat(view.currentState(), is(OnboardingState.REGISTERED));
        assertThat(view.currentStep(), is(OnboardingTrainStep.REGISTRATION));
        assertThat(view.steps().get(0).status(), is(OnboardingTrainStepStatus.CURRENT));
        assertThat(view.steps().get(1).status(), is(OnboardingTrainStepStatus.PENDING));
        assertThat(view.steps().get(2).status(), is(OnboardingTrainStepStatus.PENDING));
    }

    @Test
    void shouldShowIdentityCheckAsCurrentWhenEmailIsVerified() {
        String username = "email.verified.user@example.com";

        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(username));

        OnboardingTrainView view = trainService.getAuthenticatedTrain(username);

        assertThat(view.currentState(), is(OnboardingState.EMAIL_VERIFIED));
        assertThat(view.currentStep(), is(OnboardingTrainStep.IDENTITY_CHECK));
        assertThat(view.steps().get(0).status(), is(OnboardingTrainStepStatus.COMPLETED));
        assertThat(view.steps().get(1).status(), is(OnboardingTrainStepStatus.CURRENT));
        assertThat(view.steps().get(2).status(), is(OnboardingTrainStepStatus.PENDING));
    }

    @Test
    void shouldReturnNullWhenUserHasNoOnboardingProcess() {
        OnboardingTrainView view = trainService.getAuthenticatedTrain("missing.user@example.com");

        assertThat(view, is((OnboardingTrainView) null));
    }

    @Test
    void shouldReturnTrainByRegistrationId() {
        String username = "registration.user@example.com";
        String registrationId = "registration-123";

        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username, registrationId));

        Optional<OnboardingTrainView> view = trainService.getRegistrationStatus(registrationId);

        assertThat(view.isPresent(), is(true));
        assertThat(view.orElseThrow().currentStep(), is(OnboardingTrainStep.REGISTRATION));
    }
}
