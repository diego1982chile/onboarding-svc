package cl.dsoto.onboarding.events.sqs;

import cl.dsoto.onboarding.domain.OnboardingEventType;
import cl.dsoto.onboarding.events.IdentityEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SnsSqsIdentityEventParserTest {

    private final SnsSqsIdentityEventParser parser = new SnsSqsIdentityEventParser(new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

    @Test
    void shouldParseSnsNotificationWrappedInSqsBody() {
        String sqsBody = """
                {
                  "Type": "Notification",
                  "MessageId": "sns-message-1",
                  "TopicArn": "arn:aws:sns:us-east-1:000000000000:identity-events",
                  "Message": "{\\"version\\":1,\\"eventId\\":\\"event-1\\",\\"eventType\\":\\"USER_REGISTERED\\",\\"subject\\":\\"user@example.com\\",\\"occurredAt\\":\\"2026-06-03T12:00:00Z\\",\\"registrationId\\":\\"registration-1\\"}"
                }
                """;

        IdentityEventEnvelope envelope = parser.parse(sqsBody);

        assertThat(envelope.eventId(), is("event-1"));
        assertThat(envelope.eventType(), is(OnboardingEventType.USER_REGISTERED));
        assertThat(envelope.subject(), is("user@example.com"));
        assertThat(envelope.occurredAt(), is(Instant.parse("2026-06-03T12:00:00Z")));
        assertThat(envelope.registrationId(), is("registration-1"));
    }

    @Test
    void shouldParseRawIdentityEventBody() {
        String sqsBody = """
                {
                  "version": 1,
                  "eventId": "event-2",
                  "eventType": "EMAIL_VERIFIED",
                  "subject": "verified@example.com",
                  "occurredAt": "2026-06-04T01:00:00Z"
                }
                """;

        IdentityEventEnvelope envelope = parser.parse(sqsBody);

        assertThat(envelope.eventId(), is("event-2"));
        assertThat(envelope.eventType(), is(OnboardingEventType.EMAIL_VERIFIED));
        assertThat(envelope.subject(), is("verified@example.com"));
        assertThat(envelope.occurredAt(), is(Instant.parse("2026-06-04T01:00:00Z")));
    }
}
