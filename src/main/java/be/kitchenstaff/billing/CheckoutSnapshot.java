package be.kitchenstaff.billing;

public record CheckoutSnapshot(String id, String url, String status, String subscriptionId,
                               String customerId, String attemptId, Long establishmentId, boolean livemode) {}
