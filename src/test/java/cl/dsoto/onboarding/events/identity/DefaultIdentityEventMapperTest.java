package cl.dsoto.onboarding.events.identity;

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
    void shouldMapUserRegisteredIdentityEventToDomainEvent() {
        Instant occurredAt = Instant.parse("2026-06-03T12:00:00Z");
        IdentityEvent identityEvent = new IdentityEvent(
                "event-123",
                OnboardingEventType.USER_REGISTERED,
                "user@example.com",
                occurredAt,
                "registration-123"
        );

        OnboardingEvent event = mapper.toOnboardingEvent(identityEvent);

        assertThat(event.username(), is("user@example.com"));
        assertThat(event.type(), is(OnboardingEventType.USER_REGISTERED));
        assertThat(event.occurredAt(), is(occurredAt));
        assertThat(event.registrationId(), is("registration-123"));
    }

    @Test
    void shouldMapEmailVerifiedIdentityEventToDomainEvent() {
        Instant occurredAt = Instant.parse("2026-06-04T01:00:00Z");
        IdentityEvent identityEvent = new IdentityEvent(
                "event-456",
                OnboardingEventType.EMAIL_VERIFIED,
                "verified@example.com",
                occurredAt,
                null
        );

        OnboardingEvent event = mapper.toOnboardingEvent(identityEvent);

        assertThat(event.username(), is("verified@example.com"));
        assertThat(event.type(), is(OnboardingEventType.EMAIL_VERIFIED));
        assertThat(event.occurredAt(), is(occurredAt));
        assertThat(event.registrationId(), is((String) null));
    }

    @Test
    void shouldRejectUnsupportedIdentityEventTypeForNow() {
        IdentityEvent identityEvent = new IdentityEvent(
                "event-789",
                OnboardingEventType.PROFILE_CREATED,
                "user@example.com",
                Instant.now(),
                null
        );

        assertThrows(IllegalArgumentException.class, () -> mapper.toOnboardingEvent(identityEvent));
    }
}
