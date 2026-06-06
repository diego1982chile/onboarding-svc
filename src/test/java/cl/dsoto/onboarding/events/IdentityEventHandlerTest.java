package cl.dsoto.onboarding.events;

import cl.dsoto.onboarding.domain.OnboardingEventType;
import cl.dsoto.onboarding.domain.OnboardingState;
import cl.dsoto.onboarding.persistence.OnboardingProcessRepository;
import cl.dsoto.onboarding.persistence.ProcessedIdentityEventRepository;
import cl.dsoto.onboarding.service.OnboardingEngine;
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
class IdentityEventHandlerTest {

    @Inject
    IdentityEventHandler handler;

    @Inject
    OnboardingEngine onboardingEngine;

    @Inject
    OnboardingProcessRepository onboardingProcessRepository;

    @Inject
    ProcessedIdentityEventRepository processedIdentityEventRepository;

    @BeforeEach
    void cleanUp() {
        QuarkusTransaction.requiringNew().run(() -> {
            processedIdentityEventRepository.deleteAll();
            onboardingProcessRepository.deleteAll();
        });
    }

    @Test
    void shouldHandleNewIdentityEventAndRememberEventId() {
        IdentityEventEnvelope envelope = envelope(
                "event-1",
                OnboardingEventType.USER_REGISTERED,
                "new.user@example.com",
                "registration-1"
        );

        boolean handled = handler.handle(envelope);

        assertThat(handled, is(true));
        assertThat(onboardingEngine.getCurrentState("new.user@example.com"), is(OnboardingState.REGISTERED));
        assertThat(processedIdentityEventRepository.existsByEventId("event-1"), is(true));
    }

    @Test
    void shouldIgnoreAlreadyProcessedIdentityEvent() {
        IdentityEventEnvelope firstEnvelope = envelope(
                "event-2",
                OnboardingEventType.USER_REGISTERED,
                "duplicate.user@example.com",
                "registration-2"
        );
        IdentityEventEnvelope duplicateEnvelope = envelope(
                "event-2",
                OnboardingEventType.EMAIL_VERIFIED,
                "duplicate.user@example.com",
                null
        );

        assertThat(handler.handle(firstEnvelope), is(true));
        assertThat(handler.handle(duplicateEnvelope), is(false));

        assertThat(onboardingEngine.getCurrentState("duplicate.user@example.com"), is(OnboardingState.REGISTERED));
        assertThat(processedIdentityEventRepository.count(), is(1L));
    }

    @Test
    void shouldNotRememberRejectedIdentityEvent() {
        IdentityEventEnvelope envelope = envelope(
                "event-3",
                OnboardingEventType.KYC_APPROVED,
                "future.user@example.com",
                null
        );

        assertThrows(IllegalArgumentException.class, () -> handler.handle(envelope));

        assertThat(processedIdentityEventRepository.existsByEventId("event-3"), is(false));
    }

    private IdentityEventEnvelope envelope(
            String eventId,
            OnboardingEventType eventType,
            String subject,
            String registrationId
    ) {
        return new IdentityEventEnvelope(
                IdentityEventEnvelope.CURRENT_VERSION,
                eventId,
                eventType,
                subject,
                Instant.parse("2026-06-03T12:00:00Z"),
                registrationId
        );
    }
}
