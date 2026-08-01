package cl.dsoto.onboarding.services.impl;

import cl.dsoto.onboarding.model.OnboardingEventType;
import cl.dsoto.onboarding.model.OnboardingState;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;

@Rule(name = "profile created", priority = 30)
public class ProfileCreatedRule {

    @Condition
    public boolean matches(
            @Fact("currentState") String currentState,
            @Fact("eventType") OnboardingEventType eventType
    ) {
        return (OnboardingState.REGISTERED.name().equals(currentState)
                || OnboardingState.EMAIL_VERIFIED.name().equals(currentState))
                && eventType == OnboardingEventType.PROFILE_CREATED;
    }

    @Action
    public void apply(Facts facts) {
        facts.put("nextState", OnboardingState.PROFILE_CREATED);
        facts.put("applied", true);
    }
}
