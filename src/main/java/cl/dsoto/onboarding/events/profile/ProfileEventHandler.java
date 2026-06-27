package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.entities.ProcessedProfileEventEntity;
import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.repositories.ProcessedProfileEventRepository;
import cl.dsoto.onboarding.services.OnboardingEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class ProfileEventHandler {

    private final ProfileEventMapper profileEventMapper;
    private final OnboardingEngine onboardingEngine;
    private final ProcessedProfileEventRepository processedProfileEventRepository;

    public ProfileEventHandler(
            ProfileEventMapper profileEventMapper,
            OnboardingEngine onboardingEngine,
            ProcessedProfileEventRepository processedProfileEventRepository
    ) {
        this.profileEventMapper = profileEventMapper;
        this.onboardingEngine = onboardingEngine;
        this.processedProfileEventRepository = processedProfileEventRepository;
    }

    @Transactional
    public boolean handle(ProfileEvent profileEvent) {
        if (profileEvent == null) {
            throw new IllegalArgumentException("profileEvent is required");
        }

        if (processedProfileEventRepository.existsByEventId(profileEvent.eventId())) {
            return false;
        }

        OnboardingEvent onboardingEvent = profileEventMapper.toOnboardingEvent(profileEvent);
        onboardingEngine.applyEvent(onboardingEvent);

        processedProfileEventRepository.save(ProcessedProfileEventEntity.from(
                profileEvent.eventId(),
                profileEvent.eventType(),
                profileEvent.subject(),
                profileEvent.occurredAt(),
                Instant.now()
        ));

        return true;
    }

    public boolean handle(ProfileEventFeedItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item is required");
        }
        return handle(item.toProfileEvent());
    }
}
