package cl.dsoto.onboarding.entities;

import cl.dsoto.onboarding.model.OnboardingEventType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PROCESSED_IDENTITY_EVENT")
public class ProcessedIdentityEventEntity {

    @Id
    private String eventId;

    @Enumerated(EnumType.STRING)
    private OnboardingEventType eventType;

    private String subject;

    private Instant occurredAt;

    private Instant processedAt;

    public static ProcessedIdentityEventEntity from(
            String eventId,
            OnboardingEventType eventType,
            String subject,
            Instant occurredAt,
            Instant processedAt
    ) {
        return ProcessedIdentityEventEntity.builder()
                .eventId(eventId)
                .eventType(eventType)
                .subject(subject)
                .occurredAt(occurredAt)
                .processedAt(processedAt)
                .build();
    }
}
