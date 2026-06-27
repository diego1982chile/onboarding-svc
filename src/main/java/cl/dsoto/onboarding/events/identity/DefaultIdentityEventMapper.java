package cl.dsoto.onboarding.events.identity;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingEventType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultIdentityEventMapper implements IdentityEventMapper {

    @Override
    public OnboardingEvent toOnboardingEvent(IdentityEvent identityEvent) {
        if (identityEvent == null) {
            throw new IllegalArgumentException("identityEvent is required");
        }

        if (identityEvent.eventType() != OnboardingEventType.USER_REGISTERED
                && identityEvent.eventType() != OnboardingEventType.EMAIL_VERIFIED) {
            throw new IllegalArgumentException("Unsupported identity event type: " + identityEvent.eventType());
        }

        return new OnboardingEvent(
                identityEvent.subject(),
                identityEvent.eventType(),
                identityEvent.occurredAt(),
                identityEvent.registrationId()
        );
    }
}
