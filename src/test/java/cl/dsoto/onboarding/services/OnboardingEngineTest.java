package cl.dsoto.onboarding.services;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingEventType;
import cl.dsoto.onboarding.model.OnboardingState;
import cl.dsoto.onboarding.repositories.OnboardingProcessRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class OnboardingEngineTest {

    @Inject
    OnboardingEngine onboardingEngine;

    @Inject
    OnboardingProcessRepository repository;

    @BeforeEach
    void cleanUp() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
    }

    @Test
    void shouldCreateRegisteredProcessWhenUserRegisters() {
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered("new.user@example.com"));

        assertThat(
                onboardingEngine.getCurrentState("new.user@example.com"),
                is(OnboardingState.REGISTERED)
        );
    }

    @Test
    void shouldAdvanceRegisteredUserToEmailVerified() {
        String username = "verified.user@example.com";

        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(username));

        assertThat(onboardingEngine.getCurrentState(username), is(OnboardingState.EMAIL_VERIFIED));
    }

    @Test
    void shouldKeepStateWhenSameEventIsAppliedTwice() {
        String username = "idempotent.user@example.com";

        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));

        assertThat(onboardingEngine.getCurrentState(username), is(OnboardingState.REGISTERED));
    }

    @Test
    void shouldRejectInvalidTransition() {
        OnboardingEvent event = new OnboardingEvent(
                "jump.user@example.com",
                OnboardingEventType.PLAN_SELECTED,
                Instant.now()
        );

        assertThrows(IllegalStateException.class, () -> onboardingEngine.applyEvent(event));
    }
}
