package be.kitchenstaff.billing;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;

@Component
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {
    private boolean enabled;
    private String secretKey = "";
    private String webhookSecret = "";
    private String publicUrl = "http://localhost:4200";
    private String recoverRejectedAttempt = "";

    @PostConstruct
    public void validate() {
        if (!secretKey.isBlank() && !isTestKey()) {
            throw new IllegalStateException("Stripe requires a test secret key; live mode is prohibited");
        }
        if (enabled && (!isTestKey() || !webhookSecret.startsWith("whsec_"))) {
            throw new IllegalStateException("Stripe test configuration requires a test key and webhook signing secret");
        }
        if (!recoverRejectedAttempt.isEmpty()) {
            try {
                if (!UUID.fromString(recoverRejectedAttempt).toString().equals(recoverRejectedAttempt)) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Stripe rejected-attempt recovery requires a canonical UUID");
            }
        }
        URI uri;
        try { uri = URI.create(publicUrl); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("Invalid application public URL"); }
        boolean localHttp = "http".equals(uri.getScheme())
                && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if (uri.getHost() == null || !("https".equals(uri.getScheme()) || localHttp)
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("Application URL must use HTTPS or local HTTP, without credentials/query/fragment");
        }
    }

    public boolean isConfigured() { return enabled && isTestKey() && webhookSecret.startsWith("whsec_"); }
    private boolean isTestKey() { return secretKey.startsWith("sk_test_") || secretKey.startsWith("rk_test_"); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey == null ? "" : secretKey.trim(); }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim(); }
    public String getPublicUrl() { return publicUrl.replaceAll("/+$", ""); }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    public String getRecoverRejectedAttempt() { return recoverRejectedAttempt; }
    public void setRecoverRejectedAttempt(String recoverRejectedAttempt) {
        this.recoverRejectedAttempt = recoverRejectedAttempt == null ? "" : recoverRejectedAttempt.trim();
    }
}
