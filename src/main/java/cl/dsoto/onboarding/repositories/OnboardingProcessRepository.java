package cl.dsoto.onboarding.repositories;

import cl.dsoto.onboarding.entities.OnboardingProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingProcessRepository extends JpaRepository<OnboardingProcessEntity, String> {

    Optional<OnboardingProcessEntity> findByUsername(String username);

    Optional<OnboardingProcessEntity> findByRegistrationId(String registrationId);
}
