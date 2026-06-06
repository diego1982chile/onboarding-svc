package cl.dsoto.onboarding.events.sqs;

import cl.dsoto.onboarding.events.IdentityEventEnvelope;
import cl.dsoto.onboarding.events.IdentityEventHandler;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@ApplicationScoped
public class SqsIdentityEventConsumer {

    private static final Logger LOGGER = Logger.getLogger(SqsIdentityEventConsumer.class);

    private final SqsClient sqsClient;
    private final SnsSqsIdentityEventParser parser;
    private final IdentityEventHandler identityEventHandler;

    @ConfigProperty(name = "identity.events.consumer.enabled")
    boolean enabled;

    @ConfigProperty(name = "identity.events.queue.name")
    String queueName;

    @ConfigProperty(name = "identity.events.sqs.max-messages")
    int maxMessages;

    @ConfigProperty(name = "identity.events.sqs.wait-time-seconds")
    int waitTimeSeconds;

    private volatile String queueUrl;

    public SqsIdentityEventConsumer(
            SqsClient sqsClient,
            SnsSqsIdentityEventParser parser,
            IdentityEventHandler identityEventHandler
    ) {
        this.sqsClient = sqsClient;
        this.parser = parser;
        this.identityEventHandler = identityEventHandler;
    }

    @Scheduled(every = "{identity.events.sqs.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        if (!enabled) {
            return;
        }

        try {
            receiveMessages().forEach(this::processMessage);
        } catch (Exception exception) {
            LOGGER.error("Unable to poll identity events from SQS", exception);
        }
    }

    List<Message> receiveMessages() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(resolveQueueUrl())
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(waitTimeSeconds)
                .build();

        return sqsClient.receiveMessage(request).messages();
    }

    void processMessage(Message message) {
        try {
            IdentityEventEnvelope envelope = parser.parse(message.body());
            boolean handled = identityEventHandler.handle(envelope);
            deleteMessage(message);

            if (handled) {
                LOGGER.infof("Processed identity event %s", envelope.eventId());
            } else {
                LOGGER.infof("Ignored duplicate identity event %s", envelope.eventId());
            }
        } catch (Exception exception) {
            LOGGER.errorf(exception, "Failed to process identity event SQS message %s", message.messageId());
        }
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(resolveQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build());
    }

    private String resolveQueueUrl() {
        if (queueUrl == null) {
            queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build()).queueUrl();
        }

        return queueUrl;
    }
}
