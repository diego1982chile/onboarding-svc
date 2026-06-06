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
        return stateProducedBy(eventType).equals(currentState);
    }

    @Action
    public void apply(Facts facts) {
        facts.put("applied", true);
    }

    private String stateProducedBy(OnboardingEventType eventType) {
        if (eventType == null) {
            return "";
        }

        return switch (eventType) {
            case USER_REGISTERED -> OnboardingState.REGISTERED.name();
            case EMAIL_VERIFIED -> OnboardingState.EMAIL_VERIFIED.name();
            case KYC_APPROVED -> OnboardingState.KYC_APPROVED.name();
            case PLAN_SELECTED -> OnboardingState.PLAN_SELECTED.name();
            case PROFILE_COMPLETED -> OnboardingState.PROFILE_COMPLETED.name();
        };
    }
}
