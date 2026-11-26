package be.kitchenstaff.billing;

import be.kitchenstaff.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "establishments")
@Getter @Setter
public class Establishment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false, unique = true)
    private User owner;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
