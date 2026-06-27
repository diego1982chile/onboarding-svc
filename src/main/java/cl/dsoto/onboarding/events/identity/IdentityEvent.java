package cl.dsoto.onboarding.events.identity;

import cl.dsoto.onboarding.model.OnboardingEventType;

import java.time.Instant;

public record IdentityEvent(
        String eventId,
        OnboardingEventType eventType,
        String subject,
        Instant occurredAt,
        String registrationId
) {

    public IdentityEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (eventType == OnboardingEventType.USER_REGISTERED
                && (registrationId == null || registrationId.isBlank())) {
            throw new IllegalArgumentException("registrationId is required for USER_REGISTERED");
        }
    }
}
