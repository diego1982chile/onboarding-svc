package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.events.feed.EventFeedItem;
import cl.dsoto.onboarding.model.OnboardingEventType;

import java.time.Instant;

public record ProfileEventFeedItem(
        Long cursor,
        String eventId,
        OnboardingEventType eventType,
        String subject,
        String profileId,
        Instant occurredAt
) implements EventFeedItem {

    public ProfileEvent toProfileEvent() {
        return new ProfileEvent(eventId, eventType, subject, profileId, occurredAt);
    }
}
