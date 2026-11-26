package be.kitchenstaff.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "billing_subscriptions")
@Getter @Setter
public class BillingSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false, unique = true)
    private Establishment establishment;
    @Column(nullable = false, length = 40)
    private String status = "none";
    @Column(length = 255, unique = true)
    private String stripeSubscriptionId;
    @Column(length = 255)
    private String stripeCustomerId;
    @Column(length = 255, unique = true)
    private String checkoutSessionId;
    @Column(length = 36)
    private String checkoutAttemptId;
    private Instant attemptStartedAt;
    @Column(nullable = false)
    private boolean cancelAtPeriodEnd;
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
