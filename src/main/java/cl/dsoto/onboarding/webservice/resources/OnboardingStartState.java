package cl.dsoto.onboarding.webservice.resources;

import cl.dsoto.onboarding.model.OnboardingState;

public enum OnboardingStartState {
    NEW,
    REGISTERED,
    EMAIL_VERIFIED,
    PROFILE_CREATED;

    public static OnboardingStartState from(OnboardingState state) {
        return OnboardingStartState.valueOf(state.name());
    }
}
