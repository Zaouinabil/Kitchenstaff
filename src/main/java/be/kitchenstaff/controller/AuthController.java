package be.kitchenstaff.controller;
import be.kitchenstaff.dto.CurrentUserResponse;
import org.springframework.security.core.Authentication;
import be.kitchenstaff.dto.LoginRequest;
import be.kitchenstaff.dto.LoginResponse;
import be.kitchenstaff.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        String email = authentication.getName();

        return authService.getCurrentUser(email);
    }
}