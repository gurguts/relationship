package org.example.clientservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "regions", "regionNames", "regionSearch",
                "businesses", "businessNames", "businessSearch",
                "routes", "routeNames", "routeSearch",
                "sources", "sourceNames", "sourceSearch",
                "statusClients", "statusClientNames", "statusClientSearch",
                "clientProducts", "clientProductNames", "clientProductSearch"
        );
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterAccess(12, TimeUnit.HOURS);
    }
}