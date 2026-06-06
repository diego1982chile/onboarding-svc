package cl.dsoto.onboarding.service;

import cl.dsoto.onboarding.domain.OnboardingEvent;
import cl.dsoto.onboarding.domain.OnboardingState;
import cl.dsoto.onboarding.persistence.OnboardingProcess;
import cl.dsoto.onboarding.persistence.OnboardingProcessRepository;
import cl.dsoto.onboarding.service.rules.EmailVerifiedRule;
import cl.dsoto.onboarding.service.rules.IdempotentOnboardingRule;
import cl.dsoto.onboarding.service.rules.UserRegisteredRule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;

import java.time.Instant;

@ApplicationScoped
public class DefaultOnboardingEngine implements OnboardingEngine {

    private static final String INITIAL_STATE = "NONE";

    private final OnboardingProcessRepository repository;

    public DefaultOnboardingEngine(OnboardingProcessRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void applyEvent(OnboardingEvent event) {
        OnboardingProcess process = repository.findByUsername(event.username()).orElse(null);
        OnboardingState currentState = process == null ? null : process.getCurrentState();

        Facts facts = new Facts();
        facts.put("processExists", process != null);
        facts.put("currentState", currentState == null ? INITIAL_STATE : currentState.name());
        if (currentState != null) {
            facts.put("nextState", currentState);
        }
        facts.put("eventType", event.type());
        facts.put("applied", false);

        rulesEngine().fire(rules(), facts);

        if (!Boolean.TRUE.equals(facts.get("applied"))) {
            throw new IllegalStateException("Invalid onboarding event " + event.type()
                    + " for current state " + currentState);
        }

        OnboardingState nextState = facts.get("nextState");
        if (process == null) {
            repository.save(OnboardingProcess.create(
                    event.username(),
                    event.registrationId(),
                    nextState,
                    event.occurredAt()
            ));
        } else {
            process.setCurrentState(nextState);
            process.setUpdatedAt(Instant.now());
        }
    }

    @Override
    public OnboardingState getCurrentState(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        return repository.findByUsername(username)
                .map(OnboardingProcess::getCurrentState)
                .orElse(null);
    }

    private Rules rules() {
        Rules rules = new Rules();
        rules.register(new IdempotentOnboardingRule());
        rules.register(new UserRegisteredRule());
        rules.register(new EmailVerifiedRule());
        return rules;
    }

    private RulesEngine rulesEngine() {
        return new DefaultRulesEngine();
    }
}
