package cl.dsoto.onboarding.entities;

import cl.dsoto.onboarding.model.OnboardingState;
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
@Table(name = "ONBOARDING_PROCESS")
public class OnboardingProcess {

    @Id
    private String username;

    private String registrationId;

    @Enumerated(EnumType.STRING)
    private OnboardingState currentState;

    private Instant createdAt;

    private Instant updatedAt;

    public static OnboardingProcess create(
            String username,
            String registrationId,
            OnboardingState currentState,
            Instant createdAt
    ) {
        return OnboardingProcess.builder()
                .username(username)
                .registrationId(registrationId)
                .currentState(currentState)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
