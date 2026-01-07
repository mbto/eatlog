package com.github.mbto.eatlog;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static com.github.mbto.eatlog.service.CacheService.*;

@Configuration
@EnableCaching(proxyTargetClass = true)
public class CacheConfig implements CachingConfigurer {
    @Bean
    @Override
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        List<Triple<String, int[], Function<Long, Duration>>> paramsByCacheName = List.of(
                Triple.of(objectBySettingKeyCache,    new int[] {   32, 24 }, Duration::ofHours),
                Triple.of(observedAccountByIdCache,   new int[] {  512, 24 }, Duration::ofHours),
                Triple.of(siteVisitorsCache,          new int[] {    1,  1 }, Duration::ofHours)
        );
        for (var entry : paramsByCacheName) {
            String cacheName = entry.getLeft();
            int[] params = entry.getMiddle();
            Function<Long, Duration> durationFunc = entry.getRight();
            cacheManager.registerCustomCache(cacheName,
                    Caffeine.newBuilder()
                            .maximumSize(params[0])
                            .expireAfterWrite(durationFunc.apply((long) params[1]))
                            .recordStats()
                            .build());
        }
        return cacheManager;
    }
}