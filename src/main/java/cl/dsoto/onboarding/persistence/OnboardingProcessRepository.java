package cl.dsoto.onboarding.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class OnboardingProcessRepository implements PanacheRepositoryBase<OnboardingProcess, String> {

    public Optional<OnboardingProcess> findByUsername(String username) {
        return findByIdOptional(username);
    }

    public Optional<OnboardingProcess> findByRegistrationId(String registrationId) {
        return find("registrationId", registrationId).firstResultOptional();
    }

    public void save(OnboardingProcess process) {
        persist(process);
    }
}
