package org.example.webapp.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class StaticResourceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        
        // Добавляем заголовки для статических ресурсов
        if (isStaticResource(requestURI)) {
            // Заголовки для предотвращения кэширования HTML
            if (requestURI.endsWith(".html") || requestURI.equals("/") || 
                (!requestURI.contains(".") && !requestURI.startsWith("/api/"))) {
                httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                httpResponse.setHeader("Pragma", "no-cache");
                httpResponse.setHeader("Expires", "0");
            }
            // Заголовки для CSS и JS файлов
            else if (requestURI.endsWith(".css") || requestURI.endsWith(".js")) {
                httpResponse.setHeader("Cache-Control", "public, max-age=3600");
                httpResponse.setHeader("Vary", "Accept-Encoding");
            }
            // Заголовки для изображений и иконок
            else if (requestURI.endsWith(".png") || requestURI.endsWith(".jpg") || 
                     requestURI.endsWith(".jpeg") || requestURI.endsWith(".gif") || 
                     requestURI.endsWith(".svg") || requestURI.endsWith(".ico")) {
                httpResponse.setHeader("Cache-Control", "public, max-age=86400");
                httpResponse.setHeader("Vary", "Accept-Encoding");
            }
            
            // Заголовки для iOS Safari
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            
            // Специальные заголовки для iOS Safari
            if (isIOSSafari(httpRequest)) {
                httpResponse.setHeader("X-WebKit-CSP", "default-src 'self' 'unsafe-inline' 'unsafe-eval'");
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            }
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isStaticResource(String requestURI) {
        return requestURI.startsWith("/css/") || 
               requestURI.startsWith("/js/") || 
               requestURI.startsWith("/img/") || 
               requestURI.startsWith("/favicon/") ||
               requestURI.endsWith(".html") ||
               requestURI.equals("/") ||
               requestURI.endsWith(".ico") ||
               (!requestURI.contains(".") && !requestURI.startsWith("/api/"));
    }
    
    private boolean isIOSSafari(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && 
               userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod") &&
               userAgent.contains("Safari") && 
               !userAgent.contains("CriOS") && !userAgent.contains("FxiOS") && !userAgent.contains("OPiOS");
    }
}
