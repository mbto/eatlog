package com.github.mbto.eatlog;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.mbto.eatlog.service.CacheService.*;
import static java.util.Map.entry;

@Configuration
@EnableCaching(proxyTargetClass = true)
public class CacheConfig implements CachingConfigurer {
    @Bean
    @Override
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        List<Map.Entry<String, int[]>> paramsByCacheName = List.of(
                entry(geoInfoByGeonameIdCache,    new int[] { 1024, 24 }),
                entry(geonameIdByIpCache,         new int[] { 2048, 24 }),
                entry(objectBySettingKeyCache,    new int[] {   32, 24 }),
                entry(observedAccountByIdCache,   new int[] {  512, 24 }),
                entry(hoursCache,                 new int[] {    1,  1 })
        );
        for (Map.Entry<String, int[]> entry : paramsByCacheName) {
            String cacheName = entry.getKey();
            int[] params = entry.getValue();
            cacheManager.registerCustomCache(cacheName,
                    Caffeine.newBuilder()
                            .maximumSize(params[0])
                            .expireAfterWrite(Duration.ofHours(params[1]))
                            .recordStats()
                            .build());
        }
        return cacheManager;
    }
}