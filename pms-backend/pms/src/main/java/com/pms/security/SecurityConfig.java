package com.pms.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Defines who can access what.
 *
 * IMPORTANT LESSON CARRIED FROM TASK 2: Spring Security checks rules
 * TOP TO BOTTOM and stops at the FIRST match. If a broad wildcard rule
 * (like PUT /api/tasks/**) is listed before a specific one (like
 * PUT /api/tasks/*\/status), the specific rule never gets reached and
 * Members get blocked from updating their own task status. So below,
 * every specific rule is listed BEFORE the broader wildcard rule it
 * could otherwise be swallowed by.
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public — no token needed ──────────────────────────────
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/api-docs/**", "/v3/api-docs/**"
                        ).permitAll()

                        // ── Specific MEMBER-allowed routes — MUST come before
                        //    the broader ADMIN/MANAGER wildcard rules below ──────
                        .requestMatchers(HttpMethod.PATCH, "/api/tasks/*/status").authenticated()
                        .requestMatchers(HttpMethod.GET,   "/api/tasks/my-tasks").authenticated()
                        // New cross-project "All Tasks" search endpoint — exact
                        // path match (no trailing /**) since GET /api/tasks with
                        // no suffix is the new searchTasks() endpoint.
                        .requestMatchers(HttpMethod.GET,   "/api/tasks").authenticated()
                        .requestMatchers(HttpMethod.GET,   "/api/dashboard").authenticated()
                        .requestMatchers(HttpMethod.GET,   "/api/projects/**").authenticated()
                        .requestMatchers(HttpMethod.GET,   "/api/tasks/**").authenticated()

                        // ── ADMIN + MANAGER only — create/edit/delete ─────────────
                        .requestMatchers(HttpMethod.POST,   "/api/projects").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                        .requestMatchers(HttpMethod.PUT,    "/api/projects/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/projects/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/tasks").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                        .requestMatchers(HttpMethod.PUT,    "/api/tasks/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/tasks/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

                        // ── ADMIN only — user management (future use) ────────────
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                        // ── Everything else just needs to be logged in ───────────
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}