package cl.dsoto.onboarding.identity.events;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultIdentityEventMapperTest {

    private final IdentityEventMapper mapper = new DefaultIdentityEventMapper();

    @Test
    void shouldMapUserRegisteredEnvelopeToDomainEvent() {
        Instant occurredAt = Instant.parse("2026-06-03T12:00:00Z");
        IdentityEventEnvelope envelope = new IdentityEventEnvelope(
                1,
                "event-123",
                OnboardingEventType.USER_REGISTERED,
                "user@example.com",
                occurredAt,
                "registration-123"
        );

        OnboardingEvent event = mapper.toOnboardingEvent(envelope);

        assertThat(event.username(), is("user@example.com"));
        assertThat(event.type(), is(OnboardingEventType.USER_REGISTERED));
        assertThat(event.occurredAt(), is(occurredAt));
        assertThat(event.registrationId(), is("registration-123"));
    }

    @Test
    void shouldMapEmailVerifiedEnvelopeToDomainEvent() {
        Instant occurredAt = Instant.parse("2026-06-04T01:00:00Z");
        IdentityEventEnvelope envelope = new IdentityEventEnvelope(
                1,
                "event-456",
                OnboardingEventType.EMAIL_VERIFIED,
                "verified@example.com",
                occurredAt,
                null
        );

        OnboardingEvent event = mapper.toOnboardingEvent(envelope);

        assertThat(event.username(), is("verified@example.com"));
        assertThat(event.type(), is(OnboardingEventType.EMAIL_VERIFIED));
        assertThat(event.occurredAt(), is(occurredAt));
        assertThat(event.registrationId(), is((String) null));
    }

    @Test
    void shouldRejectUnsupportedIdentityEventTypeForNow() {
        IdentityEventEnvelope envelope = new IdentityEventEnvelope(
                1,
                "event-789",
                OnboardingEventType.KYC_APPROVED,
                "user@example.com",
                Instant.now(),
                null
        );

        assertThrows(IllegalArgumentException.class, () -> mapper.toOnboardingEvent(envelope));
    }
}
