package be.kitchenstaff.security;

import be.kitchenstaff.config.JwtAuthenticationFilter;
import be.kitchenstaff.config.CorsConfig;
import be.kitchenstaff.config.SecurityConfig;
import be.kitchenstaff.entity.User;
import be.kitchenstaff.enums.Role;
import be.kitchenstaff.repository.UserRepository;
import be.kitchenstaff.service.JwtService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.net.URI;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityAuthorizationTests {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("security-test", Map.of(
                "app.jwt.expiration-ms", "60000",
                "app.jwt.secret", UUID.randomUUID().toString() + UUID.randomUUID()
        )));
        context.register(SecurityConfig.class, CorsConfig.class, Dependencies.class);
        context.refresh();
        JwtService jwt = context.getBean(JwtService.class);
        UserRepository users = context.getBean(UserRepository.class);
        for (Role role : Role.values()) {
            String token = role.name().toLowerCase();
            User user = new User();
            user.setId((long) role.ordinal() + 1);
            user.setEmail(token + "@example.test");
            user.setRole(role);
            user.setActive(true);
            when(jwt.validateToken(token)).thenReturn(Optional.of(
                    new JwtService.TokenIdentity(user.getId(), user.getEmail())));
            when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(users.findById(user.getId())).thenReturn(Optional.of(user));
        }
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @ParameterizedTest
    @CsvSource({
            "commis, GET, /api/v1/billing/subscription, 403",
            "commis, POST, /api/v1/billing/checkout, 403",
            "commis, POST, /api/v1/billing/establishment, 403",
            "chef, GET, /api/v1/billing/subscription, 403",
            "chef, POST, /api/v1/billing/checkout, 403",
            "chef, POST, /api/v1/billing/establishment, 403",
            "admin, GET, /api/v1/billing/subscription, 200",
            "admin, POST, /api/v1/billing/checkout, 200",
            "admin, POST, /api/v1/billing/establishment, 200",
            "commis, GET, /api/v1/tasks, 200",
            "commis, GET, /api/v1/tasks/1, 200",
            "commis, PATCH, /api/v1/tasks/1/start, 200",
            "commis, PATCH, /api/v1/tasks/1/done, 200",
            "commis, POST, /api/v1/tasks, 403",
            "commis, PUT, /api/v1/tasks/1, 403",
            "commis, DELETE, /api/v1/tasks/1, 403",
            "commis, PATCH, /api/v1/tasks/1/cancel, 403",
            "commis, PATCH, /api/v1/tasks/1/unassign, 403",
            "commis, GET, /api/v1/items, 200",
            "commis, POST, /api/v1/items, 403",
            "commis, PUT, /api/v1/items/1, 403",
            "commis, PATCH, /api/v1/items/1/deactivate, 403",
            "commis, PATCH, /api/v1/items/1/reactivate, 403",
            "commis, GET, /api/v1/users/assignable, 403",
            "commis, GET, /api/v1/dashboard/today, 403",
            "chef, GET, /api/v1/users/assignable, 200",
            "chef, GET, /api/v1/users, 403",
            "chef, POST, /api/v1/users, 403",
            "chef, POST, /api/v1/tasks, 200",
            "chef, PUT, /api/v1/tasks/1, 200",
            "chef, PATCH, /api/v1/tasks/1/cancel, 200",
            "chef, DELETE, /api/v1/tasks/1, 200",
            "chef, POST, /api/v1/items, 200",
            "chef, GET, /api/v1/dashboard/today, 200",
            "admin, GET, /api/v1/users/assignable, 200",
            "admin, POST, /api/v1/users, 200",
            "admin, DELETE, /api/v1/tasks/1, 200"
    })
    void appliesRoleAndMethodRules(String role, String method, String path, int expected) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), URI.create(path))
                        .header("Authorization", "Bearer " + role))
                .andExpect(status().is(expected));
    }

    @Test
    void missingOrInvalidTokensReturnUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.POST, URI.create("/api/v1/billing/checkout"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inactiveAccountCannotUseExistingToken() throws Exception {
        UserRepository users = context.getBean(UserRepository.class);
        User commis = users.findByEmail("commis@example.test").orElseThrow();
        commis.setActive(false);
        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer commis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentDatabaseRoleOverridesRoleAtTokenIssuance() throws Exception {
        UserRepository users = context.getBean(UserRepository.class);
        User administrator = users.findByEmail("admin@example.test").orElseThrow();
        administrator.setRole(Role.COMMIS);
        mvc.perform(request(HttpMethod.POST, URI.create("/api/v1/tasks"))
                        .header("Authorization", "Bearer admin"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenIdentityMustMatchBothDatabaseIdAndEmail() throws Exception {
        JwtService jwt = context.getBean(JwtService.class);
        User administrator = context.getBean(UserRepository.class)
                .findByEmail("admin@example.test").orElseThrow();
        when(jwt.validateToken("mismatched-email")).thenReturn(Optional.of(
                new JwtService.TokenIdentity(administrator.getId(), "previous@example.test")));
        when(jwt.validateToken("missing-user")).thenReturn(Optional.of(
                new JwtService.TokenIdentity(999L, administrator.getEmail())));
        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer mismatched-email"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer missing-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginAndHealthRemainPublic() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mvc.perform(request(HttpMethod.POST, URI.create("/api/v1/stripe/webhook"))).andExpect(status().isOk());
        mvc.perform(request(HttpMethod.POST, URI.create("/api/v1/auth/login")))
                .andExpect(status().isOk());
    }

    @Configuration
    static class Dependencies {
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwt, UserRepository users) {
            return new JwtAuthenticationFilter(jwt, users);
        }
    }

    // Isolates access control from controller validation and all database operations.
    @RestController
    static class ProbeController {
        @RequestMapping("/api/v1/**")
        String respond() { return "ok"; }
    }
}
