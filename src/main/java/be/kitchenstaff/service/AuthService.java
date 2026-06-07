package be.kitchenstaff.service;

import be.kitchenstaff.dto.LoginRequest;
import be.kitchenstaff.dto.LoginResponse;
import be.kitchenstaff.entity.User;
import be.kitchenstaff.exception.InvalidCredentialsException;
import be.kitchenstaff.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Identifiants invalides"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException("Compte désactivé");
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException("Identifiants invalides");
        }

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setToken(null);
        response.setTokenType("Bearer");
        response.setMessage("Connexion réussie");

        return response;
    }
}