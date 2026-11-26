package be.kitchenstaff.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {
    Optional<Establishment> findByOwnerId(Long ownerId);
}
