package shared.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionStore sessions) throws Exception {
        return http
                // Next.js verifies exact Origin plus X-JCash-Request for mutations.
                // The loopback-only API independently requires that custom header;
                // no cross-origin access is enabled. Retain this existing CSRF contract.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .addFilterBefore(new SessionAuthenticationFilter(sessions), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/**").hasRole("USER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) ->
                                SessionAuthenticationFilter.error(response, 401, "Please sign in to continue."))
                        .accessDeniedHandler((request, response, error) ->
                                SessionAuthenticationFilter.error(response, 403, "This account cannot access that feature.")))
                .build();
    }
}
