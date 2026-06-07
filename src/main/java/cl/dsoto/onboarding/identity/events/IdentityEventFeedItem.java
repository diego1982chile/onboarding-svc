package cl.dsoto.onboarding.identity.events;

import cl.dsoto.onboarding.model.OnboardingEventType;

import java.time.Instant;

public record IdentityEventFeedItem(
        Long cursor,
        String eventId,
        OnboardingEventType eventType,
        String subject,
        String registrationId,
        Instant occurredAt
) {

    public IdentityEvent toIdentityEvent() {
        return new IdentityEvent(eventId, eventType, subject, occurredAt, registrationId);
    }
}
