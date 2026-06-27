package cl.dsoto.onboarding.events.identity;

import cl.dsoto.onboarding.model.OnboardingEvent;

public interface IdentityEventMapper {

    OnboardingEvent toOnboardingEvent(IdentityEvent identityEvent);
}
