package cl.dsoto.onboarding.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessedIdentityEventRepository implements PanacheRepositoryBase<ProcessedIdentityEvent, String> {

    public boolean existsByEventId(String eventId) {
        return findByIdOptional(eventId).isPresent();
    }

    public void save(ProcessedIdentityEvent processedEvent) {
        persist(processedEvent);
    }
}
