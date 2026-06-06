package cl.dsoto.onboarding.events;

import cl.dsoto.onboarding.domain.OnboardingEvent;
import cl.dsoto.onboarding.persistence.ProcessedIdentityEvent;
import cl.dsoto.onboarding.persistence.ProcessedIdentityEventRepository;
import cl.dsoto.onboarding.service.OnboardingEngine;
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

        processedIdentityEventRepository.save(ProcessedIdentityEvent.from(
                envelope.eventId(),
                envelope.eventType(),
                envelope.subject(),
                envelope.occurredAt(),
                Instant.now()
        ));

        return true;
    }
}
