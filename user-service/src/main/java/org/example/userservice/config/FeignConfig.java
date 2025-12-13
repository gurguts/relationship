package org.example.userservice.config;

import feign.Client;
import feign.RequestInterceptor;
import feign.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Autowired(required = false)
    private FeignTokenInterceptor feignTokenInterceptor;

    @Bean
    public Client feignClient() {
        return new OkHttpClient();
    }
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        if (feignTokenInterceptor != null) {
            return feignTokenInterceptor;
        }
        return template -> {};
    }
}