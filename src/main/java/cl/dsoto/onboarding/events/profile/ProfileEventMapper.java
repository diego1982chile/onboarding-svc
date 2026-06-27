package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.model.OnboardingEvent;

public interface ProfileEventMapper {

    OnboardingEvent toOnboardingEvent(ProfileEvent profileEvent);
}
