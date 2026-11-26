package be.kitchenstaff.billing;

public record SubscriptionSnapshot(String id, String customerId, String status, boolean cancelAtPeriodEnd,
                                   String attemptId, Long establishmentId, boolean livemode) {}
