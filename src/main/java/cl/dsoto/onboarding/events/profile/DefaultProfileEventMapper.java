package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingEventType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultProfileEventMapper implements ProfileEventMapper {

    @Override
    public OnboardingEvent toOnboardingEvent(ProfileEvent profileEvent) {
        if (profileEvent == null) {
            throw new IllegalArgumentException("profileEvent is required");
        }

        if (profileEvent.eventType() != OnboardingEventType.PROFILE_CREATED) {
            throw new IllegalArgumentException("Unsupported profile event type: " + profileEvent.eventType());
        }

        return new OnboardingEvent(
                profileEvent.subject(),
                OnboardingEventType.PROFILE_CREATED,
                profileEvent.occurredAt(),
                null
        );
    }
}
