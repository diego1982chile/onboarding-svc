package cl.dsoto.onboarding.identity.events;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.entities.ProcessedIdentityEventEntity;
import cl.dsoto.onboarding.repositories.ProcessedIdentityEventRepository;
import cl.dsoto.onboarding.services.OnboardingEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class IdentityEventHandler {

    private final IdentityEventMapper identityEventMapper;
    private final OnboardingEngine onboardingEngine;
    private final ProcessedIdentityEventRepository processedIdentityEventRepository;

    public IdentityEventHandler(
            IdentityEventMapper identityEventMapper,
            OnboardingEngine onboardingEngine,
            ProcessedIdentityEventRepository processedIdentityEventRepository
    ) {
        this.identityEventMapper = identityEventMapper;
        this.onboardingEngine = onboardingEngine;
        this.processedIdentityEventRepository = processedIdentityEventRepository;
    }

    @Transactional
    public boolean handle(IdentityEventEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("envelope is required");
        }

        if (processedIdentityEventRepository.existsByEventId(envelope.eventId())) {
            return false;
        }

        OnboardingEvent onboardingEvent = identityEventMapper.toOnboardingEvent(envelope);
        onboardingEngine.applyEvent(onboardingEvent);

        processedIdentityEventRepository.save(ProcessedIdentityEventEntity.from(
                envelope.eventId(),
                envelope.eventType(),
                envelope.subject(),
                envelope.occurredAt(),
                Instant.now()
        ));

        return true;
    }
}
