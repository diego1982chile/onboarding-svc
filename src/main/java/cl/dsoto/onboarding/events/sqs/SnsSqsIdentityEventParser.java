package cl.dsoto.onboarding.events.sqs;

import cl.dsoto.onboarding.events.IdentityEventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SnsSqsIdentityEventParser {

    private final ObjectMapper objectMapper;

    public SnsSqsIdentityEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public IdentityEventEnvelope parse(String sqsBody) {
        if (sqsBody == null || sqsBody.isBlank()) {
            throw new IllegalArgumentException("sqsBody is required");
        }

        try {
            JsonNode body = objectMapper.readTree(sqsBody);
            JsonNode message = body.get("Message");
            if (message != null && message.isTextual()) {
                return objectMapper.readValue(message.asText(), IdentityEventEnvelope.class);
            }

            return objectMapper.treeToValue(body, IdentityEventEnvelope.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse identity event from SQS message", exception);
        }
    }
}
