package be.kitchenstaff.repository;

import be.kitchenstaff.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    List<User> findByActiveTrueOrderByNameAsc();
    boolean existsByEmail(String email);
}
