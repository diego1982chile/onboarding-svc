package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultProfileEventMapperTest {

    private final DefaultProfileEventMapper mapper = new DefaultProfileEventMapper();

    @Test
    void shouldMapProfileCreatedToOnboardingEvent() {
        Instant occurredAt = Instant.parse("2026-06-03T12:00:00Z");
        ProfileEvent profileEvent = new ProfileEvent(
                "profile-event-1",
                OnboardingEventType.PROFILE_CREATED,
                "user@example.com",
                "profile-1",
                occurredAt
        );

        OnboardingEvent onboardingEvent = mapper.toOnboardingEvent(profileEvent);

        assertThat(onboardingEvent.username(), is("user@example.com"));
        assertThat(onboardingEvent.type(), is(OnboardingEventType.PROFILE_CREATED));
        assertThat(onboardingEvent.occurredAt(), is(occurredAt));
    }

    @Test
    void shouldRejectUnsupportedProfileEventType() {
        ProfileEvent profileEvent = new ProfileEvent(
                "profile-event-2",
                OnboardingEventType.EMAIL_VERIFIED,
                "user@example.com",
                "profile-1",
                Instant.now()
        );

        assertThrows(IllegalArgumentException.class, () -> mapper.toOnboardingEvent(profileEvent));
    }
}
