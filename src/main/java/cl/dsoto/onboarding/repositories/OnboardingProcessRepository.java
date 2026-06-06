package cl.dsoto.onboarding.repositories;

import cl.dsoto.onboarding.entities.OnboardingProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingProcessRepository extends JpaRepository<OnboardingProcess, String> {

    Optional<OnboardingProcess> findByUsername(String username);

    Optional<OnboardingProcess> findByRegistrationId(String registrationId);
}
