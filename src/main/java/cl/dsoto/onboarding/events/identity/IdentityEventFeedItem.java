package cl.dsoto.onboarding.events.identity;

import cl.dsoto.onboarding.events.feed.EventFeedItem;
import cl.dsoto.onboarding.model.OnboardingEventType;

import java.time.Instant;

public record IdentityEventFeedItem(
        Long cursor,
        String eventId,
        OnboardingEventType eventType,
        String subject,
        String registrationId,
        Instant occurredAt
) implements EventFeedItem {

    public IdentityEvent toIdentityEvent() {
        return new IdentityEvent(eventId, eventType, subject, occurredAt, registrationId);
    }
}
