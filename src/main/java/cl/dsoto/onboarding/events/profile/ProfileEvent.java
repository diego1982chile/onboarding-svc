package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.model.OnboardingEventType;

import java.time.Instant;

public record ProfileEvent(
        String eventId,
        OnboardingEventType eventType,
        String subject,
        String profileId,
        Instant occurredAt
) {
}
