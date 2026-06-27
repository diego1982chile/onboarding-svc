package cl.dsoto.onboarding.services.impl;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingState;
import cl.dsoto.onboarding.entities.OnboardingProcessEntity;
import cl.dsoto.onboarding.repositories.OnboardingProcessRepository;
import cl.dsoto.onboarding.services.OnboardingEngine;
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
        OnboardingProcessEntity process = repository.findByUsername(event.username()).orElse(null);
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
            repository.save(OnboardingProcessEntity.builder()
                    .username(event.username())
                    .registrationId(event.registrationId())
                    .currentState(nextState)
                    .createdAt(event.occurredAt())
                    .updatedAt(event.occurredAt())
                    .build());
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
                .map(OnboardingProcessEntity::getCurrentState)
                .orElse(null);
    }

    private Rules rules() {
        Rules rules = new Rules();
        rules.register(new IdempotentOnboardingRule());
        rules.register(new UserRegisteredRule());
        rules.register(new EmailVerifiedRule());
        rules.register(new ProfileCreatedRule());
        return rules;
    }

    private RulesEngine rulesEngine() {
        return new DefaultRulesEngine();
    }
}
