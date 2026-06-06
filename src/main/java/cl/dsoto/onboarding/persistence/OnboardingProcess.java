package cl.dsoto.onboarding.persistence;

import cl.dsoto.onboarding.domain.OnboardingState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public OnboardingState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(OnboardingState currentState) {
        this.currentState = currentState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static OnboardingProcess create(
            String username,
            String registrationId,
            OnboardingState currentState,
            Instant createdAt
    ) {
        OnboardingProcess process = new OnboardingProcess();
        process.setUsername(username);
        process.setRegistrationId(registrationId);
        process.setCurrentState(currentState);
        process.setCreatedAt(createdAt);
        process.setUpdatedAt(createdAt);
        return process;
    }
}
