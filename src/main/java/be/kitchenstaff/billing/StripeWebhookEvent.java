package be.kitchenstaff.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "stripe_webhook_events")
@Getter @Setter
public class StripeWebhookEvent {
    @Id @Column(length = 255)
    private String id;
    @Column(nullable = false, length = 100)
    private String type;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;
    @Column(nullable = false, updatable = false)
    private Instant processedAt = Instant.now();
}
