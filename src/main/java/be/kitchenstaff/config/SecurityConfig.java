package be.kitchenstaff.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> authentication;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/stripe/webhook").permitAll()
                        .requestMatchers("/api/v1/billing/**").hasRole("ADMIN")

                        // Liste minimale pour attribuer les tâches, sans accès à l'administration.
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/assignable").hasAnyRole("ADMIN", "CHEF")
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        // Catégories et préparations : chef ou admin
                        .requestMatchers("/api/v1/categories/**").hasAnyRole("ADMIN", "CHEF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/items/**").hasAnyRole("ADMIN", "CHEF", "COMMIS")
                        .requestMatchers("/api/v1/items/**").hasAnyRole("ADMIN", "CHEF")

                        // Dashboard : chef ou admin
                        .requestMatchers("/api/v1/dashboard/**").hasAnyRole("ADMIN", "CHEF")

                        // Le service limite les lectures et l'avancement du commis à ses tâches.
                        .requestMatchers(HttpMethod.GET, "/api/v1/tasks/**").hasAnyRole("ADMIN", "CHEF", "COMMIS")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/tasks/*/start", "/api/v1/tasks/*/done")
                        .hasAnyRole("ADMIN", "CHEF", "COMMIS")
                        .requestMatchers("/api/v1/tasks/**").hasAnyRole("ADMIN", "CHEF")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }
}
