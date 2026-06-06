package cl.dsoto.onboarding.persistence;

import cl.dsoto.onboarding.domain.OnboardingEventType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "PROCESSED_IDENTITY_EVENT")
public class ProcessedIdentityEvent {

    @Id
    private String eventId;

    @Enumerated(EnumType.STRING)
    private OnboardingEventType eventType;

    private String subject;

    private Instant occurredAt;

    private Instant processedAt;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public OnboardingEventType getEventType() {
        return eventType;
    }

    public void setEventType(OnboardingEventType eventType) {
        this.eventType = eventType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public static ProcessedIdentityEvent from(
            String eventId,
            OnboardingEventType eventType,
            String subject,
            Instant occurredAt,
            Instant processedAt
    ) {
        ProcessedIdentityEvent processedEvent = new ProcessedIdentityEvent();
        processedEvent.setEventId(eventId);
        processedEvent.setEventType(eventType);
        processedEvent.setSubject(subject);
        processedEvent.setOccurredAt(occurredAt);
        processedEvent.setProcessedAt(processedAt);
        return processedEvent;
    }
}
