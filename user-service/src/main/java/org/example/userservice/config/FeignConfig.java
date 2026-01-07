package org.example.userservice.config;

import feign.RequestInterceptor;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {
    
    @Bean
    public RequestInterceptor requestInterceptor(@NonNull FeignTokenInterceptor feignTokenInterceptor) {
        return feignTokenInterceptor;
    }
}