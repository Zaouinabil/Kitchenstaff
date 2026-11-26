package be.kitchenstaff.billing;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@Service
public class StripeWebhookService {
    private static final Set<String> SUPPORTED = Set.of("checkout.session.completed", "checkout.session.expired",
            "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted",
            "invoice.paid", "invoice.payment_failed");
    private static final Set<String> STATUSES = Set.of("incomplete", "incomplete_expired", "trialing", "active",
            "past_due", "canceled", "unpaid", "paused");
    private final StripeGateway stripe;
    private final BillingSubscriptionRepository subscriptions;
    private final StripeWebhookEventRepository events;
    private final TransactionTemplate transactions;

    public StripeWebhookService(StripeGateway stripe, BillingSubscriptionRepository subscriptions,
                                StripeWebhookEventRepository events, PlatformTransactionManager transactionManager) {
        this.stripe = stripe;
        this.subscriptions = subscriptions;
        this.events = events;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public void handle(String raw, String signature) {
        StripeEventSnapshot event = stripe.verifyEvent(raw, signature);
        if (event.livemode()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Live events are disabled");
        }
        if (!SUPPORTED.contains(event.type()) || events.existsById(event.id())) return;
        Long establishmentId = event.establishmentId();
        String attemptId = event.attemptId();
        // Invoice snapshots can lack metadata. Resolve the subscription, but retrieve it AGAIN
        // after locking our row: a late snapshot must not roll back a newer subscription state.
        if ((establishmentId == null || attemptId == null) && event.subscriptionId() != null) {
            SubscriptionSnapshot current = stripe.retrieveSubscription(event.subscriptionId());
            requireTest(current.livemode());
            establishmentId = current.establishmentId();
            attemptId = current.attemptId();
        }
        if (establishmentId == null || attemptId == null) return;
        Long targetId = establishmentId;
        String targetAttempt = attemptId;
        try {
            transactions.executeWithoutResult(ignored -> process(event, targetId, targetAttempt));
        } catch (DataIntegrityViolationException duplicate) {
            // A concurrent delivery may have committed this event while ours was waiting.
            // No other integrity error is swallowed; the transaction has already rolled back.
            if (!events.existsById(event.id())) throw duplicate;
        }
    }

    private void process(StripeEventSnapshot event, Long establishmentId, String attemptId) {
        BillingSubscription local = subscriptions.lockByEstablishmentId(establishmentId).orElse(null);
        if (local == null || events.existsById(event.id())) return;
        if (!Objects.equals(local.getCheckoutAttemptId(), attemptId)) {
            record(event, local);
            return;
        }
        String subscriptionId = event.subscriptionId();
        if (event.type().startsWith("checkout.session.")) {
            CheckoutSnapshot checkout = stripe.retrieveCheckout(event.objectId());
            requireTest(checkout.livemode());
            if (!matches(checkout.establishmentId(), checkout.attemptId(), local)
                    || (local.getCheckoutSessionId() != null && !local.getCheckoutSessionId().equals(checkout.id()))) {
                record(event, local);
                return;
            }
            if ("expired".equals(checkout.status())) {
                if (local.getStripeSubscriptionId() == null) {
                    local.setStatus("none");
                    local.setCheckoutSessionId(null);
                    local.setCheckoutAttemptId(null);
                    local.setAttemptStartedAt(null);
                    local.setUpdatedAt(Instant.now());
                }
                record(event, local);
                return;
            }
            if (!"complete".equals(checkout.status()) || checkout.subscriptionId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout confirmation is not ready");
            }
            local.setCheckoutSessionId(checkout.id());
            subscriptionId = checkout.subscriptionId();
        }
        if (subscriptionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription identity is missing");
        }
        SubscriptionSnapshot current = stripe.retrieveSubscription(subscriptionId);
        requireTest(current.livemode());
        if (!matches(current.establishmentId(), current.attemptId(), local)
                || !subscriptionId.equals(current.id())
                || (local.getStripeSubscriptionId() != null && !local.getStripeSubscriptionId().equals(current.id()))) {
            record(event, local);
            return;
        }
        if (!STATUSES.contains(current.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unknown Stripe subscription status");
        }
        local.setStripeSubscriptionId(current.id());
        local.setStripeCustomerId(current.customerId());
        local.setStatus(current.status());
        local.setCancelAtPeriodEnd(current.cancelAtPeriodEnd());
        local.setUpdatedAt(Instant.now());
        record(event, local);
    }

    private boolean matches(Long establishmentId, String attemptId, BillingSubscription local) {
        return Objects.equals(local.getEstablishment().getId(), establishmentId)
                && Objects.equals(local.getCheckoutAttemptId(), attemptId);
    }

    private void requireTest(boolean livemode) {
        if (livemode) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Live events are disabled");
    }

    private void record(StripeEventSnapshot event, BillingSubscription local) {
        StripeWebhookEvent processed = new StripeWebhookEvent();
        processed.setId(event.id());
        processed.setType(event.type());
        processed.setEstablishment(local.getEstablishment());
        events.saveAndFlush(processed);
    }
}
