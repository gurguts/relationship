package org.example.purchaseservice.config;

import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {
    
    @Bean
    public RequestInterceptor requestInterceptor(FeignTokenInterceptor feignTokenInterceptor) {
        return feignTokenInterceptor;
    }
    
    @Bean
    public SpringMvcContract feignContract() {
        return new SpringMvcContract();
    }
}