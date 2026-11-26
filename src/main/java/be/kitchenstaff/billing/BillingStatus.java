package be.kitchenstaff.billing;

public record BillingStatus(boolean configured, boolean testMode, long amount, String currency,
                            String interval, EstablishmentView establishment, String status,
                            boolean confirmed, boolean cancelAtPeriodEnd) {}
