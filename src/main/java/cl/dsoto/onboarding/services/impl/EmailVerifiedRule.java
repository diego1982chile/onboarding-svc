package cl.dsoto.onboarding.services.impl;

import cl.dsoto.onboarding.model.OnboardingEventType;
import cl.dsoto.onboarding.model.OnboardingState;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;

@Rule(name = "email verified", priority = 20)
public class EmailVerifiedRule {

    @Condition
    public boolean matches(
            @Fact("currentState") String currentState,
            @Fact("eventType") OnboardingEventType eventType
    ) {
        return OnboardingState.REGISTERED.name().equals(currentState)
                && eventType == OnboardingEventType.EMAIL_VERIFIED;
    }

    @Action
    public void apply(Facts facts) {
        facts.put("nextState", OnboardingState.EMAIL_VERIFIED);
        facts.put("applied", true);
    }
}
