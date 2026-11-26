package be.kitchenstaff.billing;

public record StripeEventSnapshot(String id, String type, String objectId, String subscriptionId,
                                  String attemptId, Long establishmentId, boolean livemode) {}
