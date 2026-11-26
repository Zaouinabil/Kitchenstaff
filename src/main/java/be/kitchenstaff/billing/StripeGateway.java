package be.kitchenstaff.billing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.StripeCollection;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionListParams;
import com.stripe.param.SubscriptionListParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class StripeGateway {
    public static final long MONTHLY_AMOUNT = 1990L;
    private static final Logger LOG = LoggerFactory.getLogger(StripeGateway.class);
    private final StripeProperties properties;
    private final StripeClient client;

    @Autowired
    public StripeGateway(StripeProperties properties) {
        this(properties, properties.isConfigured() ? StripeClient.builder()
                .setApiKey(properties.getSecretKey()).setConnectTimeout(5000).setReadTimeout(10000)
                .setMaxNetworkRetries(1).build() : null);
    }

    StripeGateway(StripeProperties properties, StripeClient client) {
        this.properties = properties;
        this.client = client;
    }

    public boolean isConfigured() { return properties.isConfigured(); }

    public boolean canRecoverRejectedAttempt(String attemptId) {
        String authorizedAttempt = properties.getRecoverRejectedAttempt();
        return isConfigured() && !authorizedAttempt.isBlank() && authorizedAttempt.equals(attemptId);
    }

    public CheckoutSnapshot createCheckout(Long establishmentId, String name, String attemptId) {
        requireConfigured();
        try {
            Session session = client.v1().checkout().sessions().create(checkoutParams(establishmentId, name, attemptId),
                    RequestOptions.builder().setIdempotencyKey("kitchenstaff-test-" + attemptId).build());
            return checkout(session);
        } catch (StripeException ex) { throw stripeFailure("checkout.create", ex); }
    }

    SessionCreateParams checkoutParams(Long establishmentId, String name, String attemptId) {
        Map<String, String> metadata = Map.of("establishment_id", establishmentId.toString(), "checkout_attempt_id", attemptId);
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                // Account defaults may enable Managed Payments, which rejects card filtering.
                .setManagedPayments(SessionCreateParams.ManagedPayments.builder().setEnabled(false).build())
                .addAllowedPaymentMethodType(SessionCreateParams.AllowedPaymentMethodType.CARD)
                .setSuccessUrl(properties.getPublicUrl() + "/billing/success")
                .setCancelUrl(properties.getPublicUrl() + "/billing/cancel")
                .setClientReferenceId(establishmentId.toString())
                .putAllMetadata(metadata)
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder().putAllMetadata(metadata).build())
                .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur").setUnitAmount(MONTHLY_AMOUNT)
                                .setTaxBehavior(SessionCreateParams.LineItem.PriceData.TaxBehavior.EXCLUSIVE)
                                .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                        .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH).build())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Kitchenstaff Pro").build()).build()).build())
                .build();
    }

    public CheckoutSnapshot retrieveCheckout(String sessionId) {
        requireConfigured();
        try { return checkout(client.v1().checkout().sessions().retrieve(sessionId)); }
        catch (StripeException ex) { throw stripeFailure("checkout.retrieve", ex); }
    }

    /** Read-only reconciliation on the configured test account; incomplete scans never mean absence. */
    public Optional<CheckoutSnapshot> reconcileCheckout(Long establishmentId, String attemptId) {
        requireConfigured();
        if (establishmentId == null || attemptId == null || attemptId.isBlank()) throw reconciliationRequired();
        rejectExistingSubscriptions(establishmentId, attemptId);
        CheckoutSnapshot matching = null;
        Set<String> seen = new HashSet<>();
        String cursor = null;
        try {
            for (int pageNumber = 0; pageNumber < 10; pageNumber++) {
                var page = client.v1().checkout().sessions().list(SessionListParams.builder()
                        .setLimit(100L).setStartingAfter(cursor).build());
                requireCompletePageShape(page);
                for (Session session : page.getData()) {
                    if (session == null || session.getId() == null || session.getId().isBlank()
                            || !seen.add(session.getId())) throw reconciliationRequired();
                    requireTest(session.getLivemode());
                    cursor = session.getId();
                    Map<String, String> metadata = session.getMetadata() == null ? Map.of() : session.getMetadata();
                    boolean sameEstablishment = Objects.equals(establishmentId, establishmentId(metadata));
                    boolean sameAttempt = attemptId.equals(metadata.get("checkout_attempt_id"));
                    boolean sameReference = establishmentId.toString().equals(session.getClientReferenceId());
                    if (!sameEstablishment && !sameAttempt && !sameReference) continue;
                    if (!"subscription".equals(session.getMode())) throw reconciliationRequired();
                    if (!sameAttempt) {
                        if ("expired".equals(session.getStatus()) && session.getSubscription() == null) continue;
                        throw reconciliationRequired();
                    }
                    if (!sameEstablishment || (session.getClientReferenceId() != null && !sameReference)
                            || matching != null || session.getStatus() == null
                            || !Set.of("open", "complete", "expired").contains(session.getStatus())
                            || (!"complete".equals(session.getStatus()) && session.getSubscription() != null)) {
                        throw reconciliationRequired();
                    }
                    matching = checkout(session);
                    if ("open".equals(matching.status()) && matching.url() == null) throw reconciliationRequired();
                }
                if (!page.getHasMore()) return Optional.ofNullable(matching);
            }
            throw reconciliationRequired();
        } catch (StripeException ex) { throw stripeFailure("checkout.list", ex); }
    }

    private void rejectExistingSubscriptions(Long establishmentId, String attemptId) {
        Set<String> seen = new HashSet<>();
        String cursor = null;
        try {
            for (int pageNumber = 0; pageNumber < 10; pageNumber++) {
                var page = client.v1().subscriptions().list(SubscriptionListParams.builder()
                        .setStatus(SubscriptionListParams.Status.ALL).setLimit(100L).setStartingAfter(cursor).build());
                requireCompletePageShape(page);
                for (Subscription subscription : page.getData()) {
                    if (subscription == null || subscription.getId() == null || subscription.getId().isBlank()
                            || !seen.add(subscription.getId())) throw reconciliationRequired();
                    requireTest(subscription.getLivemode());
                    cursor = subscription.getId();
                    Map<String, String> metadata = subscription.getMetadata() == null ? Map.of() : subscription.getMetadata();
                    if (Objects.equals(establishmentId, establishmentId(metadata))
                            || attemptId.equals(metadata.get("checkout_attempt_id"))) throw reconciliationRequired();
                }
                if (!page.getHasMore()) return;
            }
            throw reconciliationRequired();
        } catch (StripeException ex) { throw stripeFailure("subscription.list", ex); }
    }

    private void requireCompletePageShape(StripeCollection<?> page) {
        if (page == null || page.getData() == null || page.getData().size() > 100 || page.getHasMore() == null
                || (page.getHasMore() && page.getData().isEmpty())) throw reconciliationRequired();
    }

    private ResponseStatusException reconciliationRequired() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Stripe reconciliation is ambiguous or an existing subscription requires confirmation");
    }

    public SubscriptionSnapshot retrieveSubscription(String subscriptionId) {
        requireConfigured();
        try {
            Subscription subscription = client.v1().subscriptions().retrieve(subscriptionId);
            requireTest(subscription.getLivemode());
            Map<String, String> metadata = subscription.getMetadata() == null ? Map.of() : subscription.getMetadata();
            if (establishmentId(metadata) == null || metadata.get("checkout_attempt_id") == null) {
                return new SubscriptionSnapshot(subscription.getId(), subscription.getCustomer(), subscription.getStatus(),
                        Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()), null, null, false);
            }
            // Reject subscriptions for a different price, quantity or recurrence, even on the same Stripe account.
            if (subscription.getItems() == null || subscription.getItems().getData() == null || subscription.getItems().getData().size() != 1) throw wrongPlan();
            var item = subscription.getItems().getData().getFirst();
            var price = item.getPrice();
            if (price == null || !Long.valueOf(1).equals(item.getQuantity())
                    || !Long.valueOf(MONTHLY_AMOUNT).equals(price.getUnitAmount()) || !"eur".equals(price.getCurrency())
                    || !"exclusive".equals(price.getTaxBehavior()) || price.getRecurring() == null
                    || !"month".equals(price.getRecurring().getInterval())
                    || !Long.valueOf(1).equals(price.getRecurring().getIntervalCount())) throw wrongPlan();
            return new SubscriptionSnapshot(subscription.getId(), subscription.getCustomer(), subscription.getStatus(),
                    Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()),
                    metadata.get("checkout_attempt_id"),
                    establishmentId(metadata), false);
        } catch (StripeException ex) { throw stripeFailure("subscription.retrieve", ex); }
    }

    public StripeEventSnapshot verifyEvent(String raw, String signature) {
        requireConfigured();
        if (raw == null || raw.length() > 1_000_000 || signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook");
        }
        try {
            Event event = Webhook.constructEvent(raw, signature, properties.getWebhookSecret());
            requireTest(event.getLivemode());
            if (event.getId() == null || event.getType() == null) throw new IllegalArgumentException();
            // Read only stable identifiers from a signed snapshot; never trust its status/order.
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("object");
            String objectId = string(object, "id");
            String subscriptionId = event.getType().startsWith("customer.subscription.") ? objectId : string(object, "subscription");
            JsonObject metadata = object.has("metadata") && object.get("metadata").isJsonObject() ? object.getAsJsonObject("metadata") : new JsonObject();
            if (event.getType().startsWith("invoice.") && object.has("parent") && object.get("parent").isJsonObject()) {
                JsonObject parent = object.getAsJsonObject("parent");
                if (parent.has("subscription_details") && parent.get("subscription_details").isJsonObject()) {
                    JsonObject details = parent.getAsJsonObject("subscription_details");
                    subscriptionId = string(details, "subscription");
                    if (details.has("metadata") && details.get("metadata").isJsonObject()) metadata = details.getAsJsonObject("metadata");
                }
            }
            String establishment = string(metadata, "establishment_id");
            return new StripeEventSnapshot(event.getId(), event.getType(), objectId, subscriptionId,
                    string(metadata, "checkout_attempt_id"), establishment == null ? null : Long.valueOf(establishment), false);
        } catch (com.stripe.exception.SignatureVerificationException | com.google.gson.JsonParseException | IllegalArgumentException | IllegalStateException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook");
        }
    }

    private CheckoutSnapshot checkout(Session session) {
        requireTest(session.getLivemode());
        if (!"subscription".equals(session.getMode())) throw wrongPlan();
        Map<String, String> metadata = session.getMetadata() == null ? Map.of() : session.getMetadata();
        if (session.getId() == null) throw wrongPlan();
        if (session.getUrl() != null) {
            URI url = URI.create(session.getUrl());
            if (!"https".equals(url.getScheme()) || !"checkout.stripe.com".equals(url.getHost()) || url.getUserInfo() != null) throw wrongPlan();
        }
        return new CheckoutSnapshot(session.getId(), session.getUrl(), session.getStatus(), session.getSubscription(),
                session.getCustomer(), metadata.get("checkout_attempt_id"), establishmentId(metadata), false);
    }

    private Long establishmentId(Map<String, String> metadata) {
        try { return metadata == null ? null : Long.valueOf(metadata.get("establishment_id")); }
        catch (NumberFormatException ex) { return null; }
    }

    private String string(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    private void requireConfigured() {
        if (!isConfigured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe test is not configured");
    }

    private void requireTest(Boolean livemode) {
        if (!Boolean.FALSE.equals(livemode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Stripe test objects are accepted");
    }

    private ResponseStatusException wrongPlan() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected Stripe subscription configuration");
    }

    private ResponseStatusException stripeFailure(String operation, StripeException ex) {
        var error = ex.getStripeError();
        String param = error != null ? error.getParam() : null;
        if (param == null && ex instanceof InvalidRequestException invalid) param = invalid.getParam();
        // Allowlisted metadata only. Never log the exception/message/cause, API key,
        // full StripeError, request/response body, Checkout URL or payment details.
        LOG.warn("Stripe API error operation={} exception={} type={} httpStatus={} code={} param={} requestId={}",
                operation, ex.getClass().getSimpleName(),
                safeToken(error == null ? null : error.getType()),
                ex.getStatusCode() == null || ex.getStatusCode() == 0 ? "none" : ex.getStatusCode(),
                safeToken(ex.getCode()), safeParam(param), safeRequestId(ex.getRequestId()));
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Stripe request failed; check the server diagnostic and retry the same checkout");
    }

    private String safeToken(String value) {
        if (value == null) return "none";
        return value.matches("[a-z][a-z0-9_]{0,79}") && !looksSensitive(value) ? value : "redacted";
    }

    private String safeParam(String value) {
        if (value == null) return "none";
        return value.matches("[a-zA-Z][a-zA-Z0-9_\\[\\].]{0,119}") && !looksSensitive(value) ? value : "redacted";
    }

    private boolean looksSensitive(String value) {
        return value.contains("sk_") || value.contains("rk_") || value.contains("pk_") || value.contains("whsec_");
    }

    private String safeRequestId(String value) {
        if (value == null) return "none";
        return value.matches("req_[A-Za-z0-9]{1,100}") ? value : "redacted";
    }
}
