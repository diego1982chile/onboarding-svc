package cl.dsoto.onboarding.identity.events;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingEventType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultIdentityEventMapper implements IdentityEventMapper {

    @Override
    public OnboardingEvent toOnboardingEvent(IdentityEventEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("envelope is required");
        }

        if (envelope.eventType() != OnboardingEventType.USER_REGISTERED
                && envelope.eventType() != OnboardingEventType.EMAIL_VERIFIED) {
            throw new IllegalArgumentException("Unsupported identity event type: " + envelope.eventType());
        }

        return new OnboardingEvent(
                envelope.subject(),
                envelope.eventType(),
                envelope.occurredAt(),
                envelope.registrationId()
        );
    }
}
