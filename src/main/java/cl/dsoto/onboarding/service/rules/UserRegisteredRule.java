package cl.dsoto.onboarding.service.rules;

import cl.dsoto.onboarding.domain.OnboardingEventType;
import cl.dsoto.onboarding.domain.OnboardingState;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;

@Rule(name = "user registered", priority = 10)
public class UserRegisteredRule {

    @Condition
    public boolean matches(
            @Fact("processExists") boolean processExists,
            @Fact("eventType") OnboardingEventType eventType
    ) {
        return !processExists && eventType == OnboardingEventType.USER_REGISTERED;
    }

    @Action
    public void apply(Facts facts) {
        facts.put("nextState", OnboardingState.REGISTERED);
        facts.put("applied", true);
    }
}
