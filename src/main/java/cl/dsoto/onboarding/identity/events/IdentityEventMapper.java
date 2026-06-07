package cl.dsoto.onboarding.identity.events;

import cl.dsoto.onboarding.model.OnboardingEvent;

public interface IdentityEventMapper {

    OnboardingEvent toOnboardingEvent(IdentityEvent identityEvent);
}
