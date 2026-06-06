package cl.dsoto.onboarding.services;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingState;

public interface OnboardingEngine {

    void applyEvent(OnboardingEvent event);

    OnboardingState getCurrentState(String username);
}
