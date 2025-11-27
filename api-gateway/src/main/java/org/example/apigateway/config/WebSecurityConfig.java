package org.example.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.example.apigateway.security.InvalidRequestFilter;
import org.example.apigateway.security.JwtReactiveFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtReactiveFilter jwtReactiveFilter;
    private final InvalidRequestFilter invalidRequestFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/login",
                                "/api/v1/auth/**",
                                "/favicon.ico",
                                "/api/v1/user/auth/**",
                                "/api/v1/user/details/**",
                                "/favicon/**",
                                "/favicon/site.webmanifest",
                                "/js/login.js",
                                "/css/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(invalidRequestFilter, SecurityWebFiltersOrder.FIRST)
                .addFilterBefore(jwtReactiveFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService() {
        return _ -> Mono.error(new UsernameNotFoundException("UserDetailsService is not used in the api gateway"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}