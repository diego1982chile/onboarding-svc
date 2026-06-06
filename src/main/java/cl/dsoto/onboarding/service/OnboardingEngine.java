package cl.dsoto.onboarding.service;

import cl.dsoto.onboarding.domain.OnboardingEvent;
import cl.dsoto.onboarding.domain.OnboardingState;

public interface OnboardingEngine {

    void applyEvent(OnboardingEvent event);

    OnboardingState getCurrentState(String username);
}
