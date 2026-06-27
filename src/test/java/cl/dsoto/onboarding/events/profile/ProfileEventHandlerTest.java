package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.model.OnboardingEvent;
import cl.dsoto.onboarding.model.OnboardingEventType;
import cl.dsoto.onboarding.model.OnboardingState;
import cl.dsoto.onboarding.repositories.OnboardingProcessRepository;
import cl.dsoto.onboarding.repositories.ProcessedProfileEventRepository;
import cl.dsoto.onboarding.services.OnboardingEngine;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class ProfileEventHandlerTest {

    @Inject
    ProfileEventHandler profileEventHandler;

    @Inject
    OnboardingEngine onboardingEngine;

    @Inject
    OnboardingProcessRepository onboardingProcessRepository;

    @Inject
    ProcessedProfileEventRepository processedProfileEventRepository;

    @BeforeEach
    void cleanUp() {
        QuarkusTransaction.requiringNew().run(() -> {
            processedProfileEventRepository.deleteAll();
            onboardingProcessRepository.deleteAll();
        });
    }

    @Test
    void shouldAdvanceEmailVerifiedUserToProfileCreated() {
        String username = "profile.feed.user@example.com";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(username));

        boolean handled = profileEventHandler.handle(profileCreated("profile-event-1", username));

        assertThat(handled, is(true));
        assertThat(onboardingEngine.getCurrentState(username), is(OnboardingState.PROFILE_CREATED));
        assertThat(processedProfileEventRepository.existsByEventId("profile-event-1"), is(true));
    }

    @Test
    void shouldIgnoreDuplicateProfileEvent() {
        String username = "duplicate.profile.feed.user@example.com";
        onboardingEngine.applyEvent(OnboardingEvent.userRegistered(username));
        onboardingEngine.applyEvent(OnboardingEvent.emailVerified(username));
        ProfileEvent event = profileCreated("profile-event-2", username);

        assertThat(profileEventHandler.handle(event), is(true));
        assertThat(profileEventHandler.handle(event), is(false));
        assertThat(onboardingEngine.getCurrentState(username), is(OnboardingState.PROFILE_CREATED));
    }

    private ProfileEvent profileCreated(String eventId, String username) {
        return new ProfileEvent(
                eventId,
                OnboardingEventType.PROFILE_CREATED,
                username,
                "profile-1",
                Instant.parse("2026-06-03T12:00:00Z")
        );
    }
}
