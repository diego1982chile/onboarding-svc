package cl.dsoto.onboarding.events.identity;

import cl.dsoto.onboarding.model.OnboardingEventType;

import java.time.Instant;

public record IdentityEventEnvelope(
        int version,
        String eventId,
        OnboardingEventType eventType,
        String subject,
        Instant occurredAt,
        String registrationId
) {

    public static final int CURRENT_VERSION = 1;

    public IdentityEventEnvelope {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported identity event version: " + version);
        }
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

    public IdentityEvent toIdentityEvent() {
        return new IdentityEvent(eventId, eventType, subject, occurredAt, registrationId);
    }
}
