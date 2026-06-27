package cl.dsoto.onboarding.services;

import cl.dsoto.onboarding.webservice.resources.OnboardingStartResource;

public interface OnboardingStartService {

    OnboardingStartResource start(String email);
}
