package cl.dsoto.onboarding.entities;

import jakarta.persistence.Entity;
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
@Table(name = "IDENTITY_EVENT_FEED_CURSOR")
public class IdentityEventFeedCursorEntity {

    @Id
    private String source;

    @Builder.Default
    private Long cursor = 0L;

    private Instant updatedAt;
}
