package be.kitchenstaff.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()

                        // Utilisateurs : réservé à l'administrateur
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        // Catégories et préparations : chef ou admin
                        .requestMatchers("/api/v1/categories/**").hasAnyRole("ADMIN", "CHEF")
                        .requestMatchers("/api/v1/items/**").hasAnyRole("ADMIN", "CHEF")

                        // Dashboard : chef ou admin
                        .requestMatchers("/api/v1/dashboard/**").hasAnyRole("ADMIN", "CHEF")

                        // Tâches : tous les utilisateurs connectés
                        .requestMatchers("/api/v1/tasks/**").hasAnyRole("ADMIN", "CHEF", "COMMIS")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }
}