package org.example.apigateway.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.config.SecurityConstants;
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
import org.springframework.util.AntPathMatcher;
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
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = exchange.getAttribute(SecurityConstants.TOKEN_ATTRIBUTE);
        if (token == null) {
            token = extractTokenFromExchange(exchange);
            if (token != null) {
                exchange.getAttributes().put(SecurityConstants.TOKEN_ATTRIBUTE, token);
            }
        }

        if (token == null || token.isEmpty()) {
            return redirectToLogin(exchange);
        }

        return validateAndAuthenticateToken(token)
                .flatMap(authentication -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .onErrorResume(AuthenticationException.class, ex -> {
                    log.debug("Authentication failed: {}", ex.getMessage());
                    return handleAuthenticationException(exchange);
                })
                .onErrorResume(NoResourceFoundException.class, ex -> {
                    log.debug("Resource not found: {}", ex.getMessage());
                    return handleResourceNotFound(exchange);
                })
                .onErrorResume(AccessDeniedException.class, ex -> {
                    log.debug("Access denied: {}", ex.getMessage());
                    return handleAccessDenied(exchange);
                });
    }

    private boolean isPublicPath(String path) {
        for (String pattern : SecurityConstants.PUBLIC_PATH_PATTERNS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> redirectToLogin(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        if (!response.isCommitted()) {
            response.setStatusCode(HttpStatus.FOUND);
            response.getHeaders().setLocation(URI.create("/login"));
        }
        return response.setComplete();
    }

    private String extractTokenFromExchange(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
        }

        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(SecurityConstants.AUTH_TOKEN_COOKIE);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }

        return null;
    }

    private Mono<Authentication> validateAndAuthenticateToken(String token) {
        return reactiveJwtTokenProvider.validateToken(token)
                .flatMap(this::createAuthentication);
    }

    private Mono<Authentication> createAuthentication(UserDTO userDto) {
        List<GrantedAuthority> authorities = userDto.authorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDto.login(), null, authorities
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