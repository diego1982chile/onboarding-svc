package cl.dsoto.onboarding.model;

import java.time.Instant;

public record OnboardingEvent(
        String username,
        OnboardingEventType type,
        Instant occurredAt,
        String registrationId
) {

    public OnboardingEvent(String username, OnboardingEventType type, Instant occurredAt) {
        this(username, type, occurredAt, null);
    }

    public OnboardingEvent {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static OnboardingEvent userRegistered(String username) {
        return userRegistered(username, null);
    }

    public static OnboardingEvent userRegistered(String username, String registrationId) {
        return new OnboardingEvent(username, OnboardingEventType.USER_REGISTERED, Instant.now(), registrationId);
    }

    public static OnboardingEvent emailVerified(String username) {
        return new OnboardingEvent(username, OnboardingEventType.EMAIL_VERIFIED, Instant.now(), null);
    }

    public static OnboardingEvent profileCreated(String username) {
        return new OnboardingEvent(username, OnboardingEventType.PROFILE_CREATED, Instant.now(), null);
    }
}
