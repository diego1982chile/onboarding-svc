package cl.dsoto.onboarding.identity.events;

import cl.dsoto.onboarding.model.OnboardingEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityEventEnvelopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldDeserializeVersionedUserRegisteredEnvelope() throws Exception {
        String json = """
                {
                  "version": 1,
                  "eventId": "event-123",
                  "eventType": "USER_REGISTERED",
                  "subject": "user@example.com",
                  "occurredAt": "2026-06-03T12:00:00Z",
                  "registrationId": "registration-123"
                }
                """;

        IdentityEventEnvelope envelope = objectMapper.readValue(json, IdentityEventEnvelope.class);

        assertThat(envelope.version(), is(1));
        assertThat(envelope.eventId(), is("event-123"));
        assertThat(envelope.eventType(), is(OnboardingEventType.USER_REGISTERED));
        assertThat(envelope.subject(), is("user@example.com"));
        assertThat(envelope.occurredAt(), is(Instant.parse("2026-06-03T12:00:00Z")));
        assertThat(envelope.registrationId(), is("registration-123"));
    }

    @Test
    void shouldSerializeEnvelopeUsingStringEventTypeAndIsoInstant() throws Exception {
        IdentityEventEnvelope envelope = new IdentityEventEnvelope(
                1,
                "event-456",
                OnboardingEventType.EMAIL_VERIFIED,
                "verified@example.com",
                Instant.parse("2026-06-04T01:00:00Z"),
                "registration-456"
        );

        String json = objectMapper.writeValueAsString(envelope);

        assertThat(json.contains("\"eventType\":\"EMAIL_VERIFIED\""), is(true));
        assertThat(json.contains("\"occurredAt\":\"2026-06-04T01:00:00Z\""), is(true));
    }

    @Test
    void shouldRejectUnsupportedVersion() {
        assertThrows(IllegalArgumentException.class, () -> new IdentityEventEnvelope(
                2,
                "event-123",
                OnboardingEventType.USER_REGISTERED,
                "user@example.com",
                Instant.now(),
                "registration-123"
        ));
    }

    @Test
    void shouldRequireRegistrationIdForUserRegistered() {
        assertThrows(IllegalArgumentException.class, () -> new IdentityEventEnvelope(
                1,
                "event-123",
                OnboardingEventType.USER_REGISTERED,
                "user@example.com",
                Instant.now(),
                null
        ));
    }

    @Test
    void shouldAllowEmailVerifiedWithoutRegistrationId() {
        IdentityEventEnvelope envelope = new IdentityEventEnvelope(
                1,
                "event-123",
                OnboardingEventType.EMAIL_VERIFIED,
                "user@example.com",
                Instant.now(),
                null
        );

        assertThat(envelope.eventType(), is(OnboardingEventType.EMAIL_VERIFIED));
    }
}
