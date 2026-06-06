package cl.dsoto.onboarding.events;

import cl.dsoto.onboarding.domain.OnboardingEvent;

public interface IdentityEventMapper {

    OnboardingEvent toOnboardingEvent(IdentityEventEnvelope envelope);
}
