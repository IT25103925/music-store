package com.sliit.musicstore.config;

import com.sliit.musicstore.entity.User;
import com.sliit.musicstore.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            String cleanEmail = email != null ? email.trim().toLowerCase() : "";
            User user = userRepository.findByEmail(cleanEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("No account found for " + email));
            UserBuilder builder = org.springframework.security.core.userdetails.User.withUsername(user.getEmail());
            builder.password(user.getPassword());
            builder.roles(user.getRole().name());
            builder.disabled(!user.isEnabled());
            return builder.build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setRequestMatcher(request -> {
            String uri = request.getRequestURI();
            String accept = request.getHeader("Accept");
            String requestedWith = request.getHeader("X-Requested-With");
            if (uri != null && (uri.contains("/api/") || uri.endsWith(".json"))) {
                return false;
            }
            if ("XMLHttpRequest".equals(requestedWith) || (accept != null && accept.contains("application/json"))) {
                return false;
            }
            return new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/**").matches(request);
        });

        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
            .requestCache(cache -> cache.requestCache(requestCache))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/home", "/register", "/login", "/access-denied", "/products", "/products/**",
                    "/rentals", "/offers", "/error", "/favicon.ico",
                    "/css/**", "/js/**", "/images/**", "/uploads/**", "/h2-console/**",
                    "/notifications/api/**", "/api/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            )
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers("/h2-console/**")
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())); // needed for H2 console

        return http.build();
    }
}
