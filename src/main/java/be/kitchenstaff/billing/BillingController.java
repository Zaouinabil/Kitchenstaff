package be.kitchenstaff.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService billing;
    public BillingController(BillingService billing) { this.billing = billing; }

    @GetMapping("/subscription")
    public BillingStatus status(Authentication authentication) {
        return billing.status(authentication.getName());
    }

    @PostMapping("/establishment")
    @ResponseStatus(HttpStatus.CREATED)
    public EstablishmentView create(@Valid @RequestBody EstablishmentRequest request, Authentication authentication) {
        return billing.createEstablishment(authentication.getName(), request.name());
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(Authentication authentication) {
        return new CheckoutResponse(billing.checkout(authentication.getName()).url());
    }

    public record EstablishmentRequest(@NotBlank @Size(max = 120) String name) {}
    public record CheckoutResponse(String url) {}
}
