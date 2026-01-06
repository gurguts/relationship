package org.example.purchaseservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    private static final int INITIAL_CAPACITY = 100;
    private static final long MAXIMUM_SIZE = 1000L;
    private static final long EXPIRE_AFTER_ACCESS_HOURS = 12L;
    private static final String[] CACHE_NAMES = {"products", "warehouses", "withdrawalReasons", 
            "exchangeRates", "sourceNames", "userFullNames"};

    @Bean
    @NonNull
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_NAMES);
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<@NonNull Object, @NonNull Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(INITIAL_CAPACITY)
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterAccess(EXPIRE_AFTER_ACCESS_HOURS, TimeUnit.HOURS);
    }
}