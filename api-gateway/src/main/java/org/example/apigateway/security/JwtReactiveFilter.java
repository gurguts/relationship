package org.example.apigateway.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.models.dto.UserDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpCookie;

import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtReactiveFilter implements WebFilter {

    private final ReactiveJwtTokenProvider reactiveJwtTokenProvider;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/login",
            "/api/v1/auth/",
            "/favicon.ico",
            "/api/v1/user/auth/",
            "/api/v1/user/details/",
            "/favicon/",
            "/favicon/site.webmanifest",
            "/js/login.js",
            "/css/"
    );

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (exchange.getAttribute("JWT_FILTER_APPLIED") != null) {
            return chain.filter(exchange);
        }
        exchange.getAttributes().put("JWT_FILTER_APPLIED", true);

        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        return extractToken(exchange)
                .switchIfEmpty(Mono.defer(() -> redirectToLogin(exchange).then(Mono.empty())))
                .flatMap(this::validateAndAuthenticateToken)
                .flatMap(authentication -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .onErrorResume(AuthenticationException.class, _ -> handleAuthenticationException(exchange))
                .onErrorResume(NoResourceFoundException.class, _ -> handleResourceNotFound(exchange))
                .onErrorResume(AccessDeniedException.class, _ -> handleAccessDenied(exchange));
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> redirectToLogin(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();

        if (!response.isCommitted()) {
            response.setStatusCode(HttpStatus.FOUND);
            response.getHeaders().setLocation(URI.create("/login"));
        }

        return response.setComplete();
    }

    private Mono<String> extractToken(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(auth -> auth.startsWith("Bearer "))
                .map(auth -> auth.substring(7))
                .switchIfEmpty(extractTokenFromCookie(exchange))
                .filter(StringUtils::hasText);
    }

    private Mono<String> extractTokenFromCookie(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getCookies().getFirst("authToken"))
                .map(HttpCookie::getValue);
    }

    private Mono<Authentication> validateAndAuthenticateToken(String token) {
        return reactiveJwtTokenProvider.validateToken(token)
                .flatMap(this::createAuthentication);
    }

    private Mono<Authentication> createAuthentication(UserDTO userDto) {
        List<GrantedAuthority> authorities = userDto.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDto.getLogin(), null, authorities
        );

        return Mono.just(authentication);
    }


    private Mono<Void> handleAuthenticationException(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return redirectToLogin(exchange);
    }

    private Mono<Void> handleResourceNotFound(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> handleAccessDenied(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

}