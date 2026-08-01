package cl.dsoto.onboarding.services.impl;

import cl.dsoto.onboarding.model.OnboardingEventType;
import cl.dsoto.onboarding.model.OnboardingState;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;

@Rule(name = "idempotent onboarding event", priority = 1)
public class IdempotentOnboardingRule {

    @Condition
    public boolean matches(
            @Fact("currentState") String currentState,
            @Fact("eventType") OnboardingEventType eventType
    ) {
        OnboardingState producedState = stateProducedBy(eventType);
        OnboardingState existingState = stateFrom(currentState);

        return producedState != null
                && existingState != null
                && producedState.ordinal() <= existingState.ordinal();
    }

    @Action
    public void apply(Facts facts) {
        facts.put("applied", true);
    }

    private OnboardingState stateProducedBy(OnboardingEventType eventType) {
        if (eventType == null) {
            return null;
        }

        return switch (eventType) {
            case USER_REGISTERED -> OnboardingState.REGISTERED;
            case EMAIL_VERIFIED -> OnboardingState.EMAIL_VERIFIED;
            case PROFILE_CREATED -> OnboardingState.PROFILE_CREATED;
        };
    }

    private OnboardingState stateFrom(String currentState) {
        if (currentState == null || currentState.isBlank()) {
            return null;
        }

        try {
            return OnboardingState.valueOf(currentState);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
