package be.kitchenstaff.billing;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stripe/webhook")
public class StripeWebhookController {
    private final StripeWebhookService webhooks;
    public StripeWebhookController(StripeWebhookService webhooks) { this.webhooks = webhooks; }

    @PostMapping(consumes = "application/json")
    public Map<String, Boolean> receive(@RequestBody String rawBody,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        webhooks.handle(rawBody, signature);
        return Map.of("received", true);
    }
}
