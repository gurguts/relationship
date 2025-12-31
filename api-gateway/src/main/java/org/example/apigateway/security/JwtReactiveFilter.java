package org.example.apigateway.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.config.SecurityConstants;
import org.example.apigateway.models.dto.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
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
            token = TokenExtractor.extractFromExchange(exchange);
            if (token != null) {
                exchange.getAttributes().put(SecurityConstants.TOKEN_ATTRIBUTE, token);
            }
        }

        if (!TokenExtractor.isValidTokenFormat(token)) {
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

    private boolean isPublicPath(@NonNull String path) {
        for (String pattern : SecurityConstants.PUBLIC_PATH_PATTERNS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> redirectToLogin(@NonNull ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        if (!response.isCommitted()) {
            response.setStatusCode(HttpStatus.FOUND);
            response.getHeaders().setLocation(URI.create("/login"));
        }
        return response.setComplete();
    }

    private Mono<Authentication> validateAndAuthenticateToken(@NonNull String token) {
        return reactiveJwtTokenProvider.validateToken(token)
                .flatMap(this::createAuthentication);
    }

    private Mono<Authentication> createAuthentication(@NonNull UserDTO userDto) {
        List<GrantedAuthority> authorities = userDto.authorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDto.login(), null, authorities
        );

        return Mono.just(authentication);
    }

    private Mono<Void> handleAuthenticationException(@NonNull ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return redirectToLogin(exchange);
    }

    private Mono<Void> handleResourceNotFound(@NonNull ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> handleAccessDenied(@NonNull ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}