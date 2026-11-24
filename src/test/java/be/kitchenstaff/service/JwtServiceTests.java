package be.kitchenstaff.service;

import be.kitchenstaff.entity.User;
import be.kitchenstaff.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTests {
    private JwtService service;
    private String signingSecret;

    @BeforeEach
    void setUp() {
        service = new JwtService();
        signingSecret = UUID.randomUUID().toString() + UUID.randomUUID();
        ReflectionTestUtils.setField(service, "jwtSecret", signingSecret);
        ReflectionTestUtils.setField(service, "jwtExpirationMs", 60_000L);
    }

    @Test
    void generatedTokenCarriesStableUserIdAndEmail() {
        User user = new User();
        user.setId(7L);
        user.setEmail("commis@example.test");
        user.setName("Commis");
        user.setRole(Role.COMMIS);
        assertEquals(new JwtService.TokenIdentity(7L, user.getEmail()),
                service.validateToken(service.generateToken(user)).orElseThrow());
    }

    @Test
    void wrongSignatureIsRejected() {
        String otherSecret = UUID.randomUUID().toString() + UUID.randomUUID();
        String token = Jwts.builder().subject("commis@example.test").claim("userId", 7L)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8))).compact();
        assertTrue(service.validateToken(token).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        String token = Jwts.builder().subject("commis@example.test").claim("userId", 7L)
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8))).compact();
        assertTrue(service.validateToken(token).isEmpty());
    }

    @Test
    void missingUserIdIsRejected() {
        String token = Jwts.builder().subject("commis@example.test")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8))).compact();
        assertTrue(service.validateToken(token).isEmpty());
    }

    @Test
    void missingEmailIsRejected() {
        String token = Jwts.builder().claim("userId", 7L)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8))).compact();
        assertTrue(service.validateToken(token).isEmpty());
    }

    @Test
    void malformedTokenIsRejected() {
        assertTrue(service.validateToken("invalid.token").isEmpty());
    }
}
