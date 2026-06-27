package cl.dsoto.onboarding.events.identity.sqs;

import cl.dsoto.onboarding.model.OnboardingEventType;
import cl.dsoto.onboarding.events.identity.IdentityEventEnvelope;
import cl.dsoto.onboarding.events.identity.IdentityEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqsIdentityEventConsumerTest {

    private final SqsClient sqsClient = mock(SqsClient.class);
    private final SnsSqsIdentityEventParser parser = mock(SnsSqsIdentityEventParser.class);
    private final IdentityEventHandler handler = mock(IdentityEventHandler.class);

    private SqsIdentityEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SqsIdentityEventConsumer(sqsClient, parser, handler);
        consumer.queueName = "onboarding-identity-events";
        consumer.maxMessages = 10;
        consumer.waitTimeSeconds = 20;

        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(GetQueueUrlResponse.builder()
                .queueUrl("http://localhost:4566/000000000000/onboarding-identity-events")
                .build());
    }

    @Test
    void shouldDeleteMessageAfterSuccessfulProcessing() {
        Message message = message("message-1", "receipt-1", "{}");
        IdentityEventEnvelope envelope = envelope("event-1");

        when(parser.parse("{}")).thenReturn(envelope);
        when(handler.handle(envelope)).thenReturn(true);

        consumer.processMessage(message);

        verify(handler).handle(envelope);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldDeleteMessageWhenEventWasAlreadyProcessed() {
        Message message = message("message-2", "receipt-2", "{}");
        IdentityEventEnvelope envelope = envelope("event-2");

        when(parser.parse("{}")).thenReturn(envelope);
        when(handler.handle(envelope)).thenReturn(false);

        consumer.processMessage(message);

        verify(handler).handle(envelope);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldLeaveMessageInQueueWhenProcessingFails() {
        Message message = message("message-3", "receipt-3", "{}");
        IdentityEventEnvelope envelope = envelope("event-3");

        when(parser.parse("{}")).thenReturn(envelope);
        when(handler.handle(envelope)).thenThrow(new IllegalStateException("processing failed"));

        consumer.processMessage(message);

        verify(handler).handle(envelope);
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    private Message message(String messageId, String receiptHandle, String body) {
        return Message.builder()
                .messageId(messageId)
                .receiptHandle(receiptHandle)
                .body(body)
                .build();
    }

    private IdentityEventEnvelope envelope(String eventId) {
        return new IdentityEventEnvelope(
                IdentityEventEnvelope.CURRENT_VERSION,
                eventId,
                OnboardingEventType.EMAIL_VERIFIED,
                "user@example.com",
                Instant.parse("2026-06-03T12:00:00Z"),
                null
        );
    }
}
