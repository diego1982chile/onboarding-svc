package cl.dsoto.onboarding.services;

import cl.dsoto.onboarding.webservice.resources.OnboardingTrainStep;
import cl.dsoto.onboarding.webservice.resources.OnboardingTrainStepStatus;
import cl.dsoto.onboarding.webservice.resources.OnboardingTrainResource;
import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingState;
import cl.dsoto.onboarding.repositories.OnboardingProcessRepository;
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
        OnboardingTrainResource view = trainService.getPublicTrain();

        assertThat(view.getUsername(), is((String) null));
        assertThat(view.getCurrentState(), is((OnboardingState) null));
        assertThat(view.getCurrentStep(), is(OnboardingTrainStep.REGISTRATION));
        assertThat(view.getSteps().get(0).getStatus(), is(OnboardingTrainStepStatus.CURRENT));
        assertThat(view.getSteps().get(1).getStatus(), is(OnboardingTrainStepStatus.PENDING));
        assertThat(view.getSteps().get(2).getStatus(), is(OnboardingTrainStepStatus.PENDING));
    }

    @Test
    void shouldShowRegistrationAsCurrentWhenUserIsRegistered() {
        String username = "registered.user@example.com";

        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));

        OnboardingTrainResource view = trainService.getAuthenticatedTrain(username);

        assertThat(view.getCurrentState(), is(OnboardingState.REGISTERED));
        assertThat(view.getCurrentStep(), is(OnboardingTrainStep.REGISTRATION));
        assertThat(view.getSteps().get(0).getStatus(), is(OnboardingTrainStepStatus.CURRENT));
        assertThat(view.getSteps().get(1).getStatus(), is(OnboardingTrainStepStatus.PENDING));
        assertThat(view.getSteps().get(2).getStatus(), is(OnboardingTrainStepStatus.PENDING));
    }

    @Test
    void shouldShowIdentityCheckAsCurrentWhenEmailIsVerified() {
        String username = "email.verified.user@example.com";

        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(username));

        OnboardingTrainResource view = trainService.getAuthenticatedTrain(username);

        assertThat(view.getCurrentState(), is(OnboardingState.EMAIL_VERIFIED));
        assertThat(view.getCurrentStep(), is(OnboardingTrainStep.IDENTITY_CHECK));
        assertThat(view.getSteps().get(0).getStatus(), is(OnboardingTrainStepStatus.COMPLETED));
        assertThat(view.getSteps().get(1).getStatus(), is(OnboardingTrainStepStatus.CURRENT));
        assertThat(view.getSteps().get(2).getStatus(), is(OnboardingTrainStepStatus.PENDING));
    }

    @Test
    void shouldReturnNullWhenUserHasNoOnboardingProcess() {
        OnboardingTrainResource view = trainService.getAuthenticatedTrain("missing.user@example.com");

        assertThat(view, is((OnboardingTrainResource) null));
    }

    @Test
    void shouldReturnTrainByRegistrationId() {
        String username = "registration.user@example.com";
        String registrationId = "registration-123";

        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username, registrationId));

        Optional<OnboardingTrainResource> view = trainService.getRegistrationStatus(registrationId);

        assertThat(view.isPresent(), is(true));
        assertThat(view.orElseThrow().getCurrentStep(), is(OnboardingTrainStep.REGISTRATION));
    }
}
