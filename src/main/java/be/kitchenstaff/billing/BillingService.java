package be.kitchenstaff.billing;

import be.kitchenstaff.entity.User;
import be.kitchenstaff.enums.Role;
import be.kitchenstaff.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class BillingService {
    private static final Duration IDEMPOTENCY_RETRY_WINDOW = Duration.ofHours(23);
    private static final Duration RECOVERY_QUIET_PERIOD = Duration.ofMinutes(5);
    private static final Set<String> RESTARTABLE = Set.of("none", "canceled", "incomplete_expired");
    private final UserRepository users;
    private final EstablishmentRepository establishments;
    private final BillingSubscriptionRepository subscriptions;
    private final StripeGateway stripe;
    private final TransactionTemplate transactions;

    public BillingService(UserRepository users, EstablishmentRepository establishments,
                          BillingSubscriptionRepository subscriptions, StripeGateway stripe,
                          PlatformTransactionManager transactionManager) {
        this.users = users;
        this.establishments = establishments;
        this.subscriptions = subscriptions;
        this.stripe = stripe;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public BillingStatus status(String email) {
        return transactions.execute(ignored -> {
            User owner = requireAdmin(email, false);
            Establishment establishment = establishments.findByOwnerId(owner.getId()).orElse(null);
            BillingSubscription subscription = establishment == null ? null
                    : subscriptions.findByEstablishmentId(establishment.getId()).orElseThrow();
            String status = subscription == null ? "none" : subscription.getStatus();
            return new BillingStatus(stripe.isConfigured(), true, 1990L, "EUR", "month",
                    establishment == null ? null : new EstablishmentView(establishment.getId(), establishment.getName()),
                    status, "active".equals(status), subscription != null && subscription.isCancelAtPeriodEnd());
        });
    }

    public EstablishmentView createEstablishment(String email, String name) {
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A name of 1 to 120 characters is required");
        }
        return transactions.execute(ignored -> {
            User owner = requireAdmin(email, true);
            Establishment existing = establishments.findByOwnerId(owner.getId()).orElse(null);
            if (existing != null) {
                if (!existing.getName().equals(name.trim())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "An establishment already exists for this administrator");
                }
                return new EstablishmentView(existing.getId(), existing.getName());
            }
            Establishment establishment = new Establishment();
            establishment.setName(name.trim());
            establishment.setOwner(owner);
            establishments.save(establishment);
            BillingSubscription subscription = new BillingSubscription();
            subscription.setEstablishment(establishment);
            subscriptions.save(subscription);
            return new EstablishmentView(establishment.getId(), establishment.getName());
        });
    }

    public CheckoutSnapshot checkout(String email) {
        if (!stripe.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe test mode is not configured");
        }
        // The reservation commits BEFORE Stripe is contacted. An uncertain network result retains
        // this UUID so a retry uses the same Stripe idempotency key, even after a process restart.
        Attempt attempt = transactions.execute(ignored -> reserve(email));
        if (attempt.sessionId() == null && (retryWindowEnded(attempt.startedAt())
                || stripe.canRecoverRejectedAttempt(attempt.id()))) {
            attempt = reconcilePending(email, attempt);
        }
        if (attempt.sessionId() != null) {
            CheckoutSnapshot existing = stripe.retrieveCheckout(attempt.sessionId());
            validateCheckout(existing, attempt);
            if ("open".equals(existing.status()) && existing.url() != null) return existing;
            if ("complete".equals(existing.status())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout completed; wait for server confirmation");
            }
            if (!"expired".equals(existing.status()) || existing.subscriptionId() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout is not available");
            }
            Attempt previous = attempt;
            attempt = transactions.execute(ignored -> {
                requireAdmin(email, true);
                BillingSubscription subscription = subscriptions.lockByEstablishmentId(previous.establishmentId()).orElseThrow();
                if (!Objects.equals(subscription.getCheckoutAttemptId(), previous.id())
                        || !"pending".equals(subscription.getStatus())
                        || subscription.getStripeSubscriptionId() != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Billing state changed; refresh first");
                }
                startAttempt(subscription);
                return attemptOf(subscription);
            });
        }
        Attempt reserved = attempt;
        // The reservation is already committed. Serialize the external create with recovery,
        // and recheck after locking so a delayed request cannot create for a replaced attempt.
        CheckoutSnapshot created = transactions.execute(ignored -> {
            requireAdmin(email, true);
            BillingSubscription subscription = subscriptions.lockByEstablishmentId(reserved.establishmentId()).orElseThrow();
            if (!Objects.equals(subscription.getCheckoutAttemptId(), reserved.id())
                    || !("pending".equals(subscription.getStatus()) || "incomplete".equals(subscription.getStatus()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Billing state changed; refresh first");
            }
            if (subscription.getCheckoutSessionId() != null) {
                CheckoutSnapshot existing = stripe.retrieveCheckout(subscription.getCheckoutSessionId());
                validateCheckout(existing, reserved);
                return existing;
            }
            if (subscription.getStripeSubscriptionId() != null || retryWindowEnded(subscription.getAttemptStartedAt())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout requires Stripe reconciliation");
            }
            CheckoutSnapshot result = stripe.createCheckout(reserved.establishmentId(), reserved.name(), reserved.id());
            validateCheckout(result, reserved);
            subscription.setCheckoutSessionId(result.id());
            subscription.setUpdatedAt(Instant.now());
            return result;
        });
        if (!"open".equals(created.status()) || created.url() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout is not open; refresh the confirmed status");
        }
        return created;
    }

    private Attempt reserve(String email) {
        User owner = requireAdmin(email, true);
        Establishment establishment = establishments.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Create an establishment first"));
        BillingSubscription subscription = subscriptions.lockByEstablishmentId(establishment.getId()).orElseThrow();
        if (RESTARTABLE.contains(subscription.getStatus())) {
            startAttempt(subscription);
        } else if (!"pending".equals(subscription.getStatus()) && !"incomplete".equals(subscription.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An existing subscription must be resolved before another checkout");
        }
        if (subscription.getCheckoutSessionId() == null
                && stripe.canRecoverRejectedAttempt(subscription.getCheckoutAttemptId())
                && (!"pending".equals(subscription.getStatus()) || subscription.getStripeCustomerId() != null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Existing billing references require manual reconciliation");
        }
        if (subscription.getCheckoutAttemptId() == null || subscription.getAttemptStartedAt() == null
                || (subscription.getCheckoutSessionId() == null && subscription.getStripeSubscriptionId() != null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An unresolved subscription requires Stripe reconciliation");
        }
        return attemptOf(subscription);
    }

    private boolean retryWindowEnded(Instant startedAt) {
        return startedAt == null || !startedAt.isAfter(Instant.now().minus(IDEMPOTENCY_RETRY_WINDOW));
    }

    private Attempt reconcilePending(String email, Attempt previous) {
        // A local operator may allow early reconciliation of ONE documented rejected attempt.
        // This is never inferred from an idempotency error, timeout or 5xx; unknown outcomes
        // retain the normal retry window. Activate the setting only after a clean restart.
        Duration minimumAge = stripe.canRecoverRejectedAttempt(previous.id())
                ? RECOVERY_QUIET_PERIOD : IDEMPOTENCY_RETRY_WINDOW.plus(RECOVERY_QUIET_PERIOD);
        if (previous.startedAt().isAfter(Instant.now().minus(minimumAge))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait a few minutes before reconciling this checkout");
        }
        var reconciled = stripe.reconcileCheckout(previous.establishmentId(), previous.id());
        if (reconciled == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe reconciliation is incomplete");
        reconciled.ifPresent(checkout -> validateCheckout(checkout, previous));
        return transactions.execute(ignored -> {
            requireAdmin(email, true);
            BillingSubscription subscription = subscriptions.lockByEstablishmentId(previous.establishmentId()).orElseThrow();
            if (!Objects.equals(subscription.getCheckoutAttemptId(), previous.id())
                    || !Objects.equals(subscription.getAttemptStartedAt(), previous.startedAt())
                    || subscription.getCheckoutSessionId() != null || subscription.getStripeSubscriptionId() != null
                    || subscription.getStripeCustomerId() != null || !"pending".equals(subscription.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Billing state changed; refresh first");
            }
            if (reconciled.isPresent()) {
                subscription.setCheckoutSessionId(reconciled.get().id());
                subscription.setUpdatedAt(Instant.now());
            } else {
                // Only an exhaustive, successful Stripe TEST read permits a fresh reservation.
                startAttempt(subscription);
            }
            return attemptOf(subscription);
        });
    }

    private void startAttempt(BillingSubscription subscription) {
        subscription.setCheckoutAttemptId(UUID.randomUUID().toString());
        subscription.setAttemptStartedAt(Instant.now());
        subscription.setCheckoutSessionId(null);
        subscription.setStripeSubscriptionId(null);
        subscription.setStripeCustomerId(null);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setStatus("pending");
        subscription.setUpdatedAt(Instant.now());
    }

    private Attempt attemptOf(BillingSubscription subscription) {
        return new Attempt(subscription.getEstablishment().getId(), subscription.getEstablishment().getName(),
                subscription.getCheckoutAttemptId(), subscription.getCheckoutSessionId(), subscription.getAttemptStartedAt());
    }

    private void validateCheckout(CheckoutSnapshot checkout, Attempt attempt) {
        if (checkout.livemode() || !Objects.equals(checkout.establishmentId(), attempt.establishmentId())
                || !Objects.equals(checkout.attemptId(), attempt.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe checkout does not match this test establishment");
        }
    }

    private User requireAdmin(String email, boolean lock) {
        User user = (lock ? users.lockByEmail(email) : users.findByEmail(email))
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()) && candidate.getRole() == Role.ADMIN)
                .orElseThrow(() -> new AccessDeniedException("An active administrator is required"));
        return user;
    }

    private record Attempt(Long establishmentId, String name, String id, String sessionId, Instant startedAt) {}
}
