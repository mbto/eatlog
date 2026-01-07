package com.github.mbto.eatlog.service;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.mbto.eatlog.common.dto.SiteVisitor;
import com.github.mbto.eatlog.common.dto.SiteVisitors;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Setting;
import com.github.mbto.eatlog.utils.DefaultContainerFactory;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.tools.json.JSONParser;
import org.jooq.types.UInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static com.github.mbto.eatlog.common.model.eatlog.tables.Account.ACCOUNT;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Setting.SETTING;

@SuppressWarnings("UnusedReturnValue")
@Service
@Lazy(false)
@Slf4j
public class CacheService {
    public static final String objectBySettingKeyCache = "objectBySettingKeyCache";
    public static final String observedAccountByIdCache = "observedAccountByIdCache";
    public static final String siteVisitorsCache = "siteVisitorsCache";
    public static final String allocateSiteVisitorsKey = "allocateSiteVisitors";

    @Autowired
    private ApplicationHolder applicationHolder;
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private CacheManager cacheManager;

    @Cacheable(cacheNames = siteVisitorsCache, key = "#root.methodName", sync = true)
    public SiteVisitors allocateSiteVisitors() {
        if(log.isDebugEnabled())
            log.debug("\nallocateSiteVisitors");
        final LocalDateTime now = LocalDateTime.now();
        int[] guestsCount = {0};
        List<SiteVisitor> authedUsers = applicationHolder.getSiteVisitorBySessionId()
                .values()
                .stream()
                .filter(siteVisitor -> {
                    if(siteVisitor.getExpiredDateTime() == null || !now.isBefore(siteVisitor.getExpiredDateTime())) {
                        return false;
                    }
                    UInteger accountId = siteVisitor.getAccountId();
                    if(accountId == null) {
                        ++guestsCount[0];
                        return false;
                    }
                    return true;
                })
                .distinct()
                .sorted(Comparator.comparing(SiteVisitor::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return new SiteVisitors(authedUsers, guestsCount[0]);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean itemExists(String cacheName, Object key) {
        return getCache(cacheName).get(key) != null;
    }

    public void evict(String cacheName, Object key) {
        evict(cacheName, key, true);
    }

    public void evict(String cacheName, Object key, boolean logMsg) {
        getCache(cacheName).evict(key);
        if(log.isDebugEnabled() || logMsg) {
            log.info("Evicted item by key=" + key + " from cacheName=" + cacheName);
        }
    }

    public void evictAll(String cacheName) {
        getCache(cacheName).invalidate();
        log.info("Evicted all items from cacheName=" + cacheName);
    }

    public long count(String cacheName) {
        return getCache(cacheName)
                .getNativeCache().estimatedSize();
    }

    public CacheStats cacheStats(String cacheName) {
        return getCache(cacheName)
                .getNativeCache().stats();
    }

    public CaffeineCache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if(!(cache instanceof CaffeineCache caffeineCache)) {
            throw new IllegalStateException("Failed getCache by cacheName=" + cacheName);
        }
        return caffeineCache;
    }

    @Cacheable(cacheNames = objectBySettingKeyCache,
            key = "{#key, #convertJsonValueToMap, #throwExceptionIfOccured, #defaultValue}",
            condition = "#key != null", sync = true)
    public Object allocateSettingByKey(String key,
                                       boolean convertJsonValueToMap,
                                       boolean throwExceptionIfOccured,
                                       String defaultValue) {
        if(log.isDebugEnabled()) {
            log.debug("\nallocateSettingByKey key=" + key
                    + ", convertJsonValueToMap=" + convertJsonValueToMap
                    + ", throwExceptionIfOccured=" + throwExceptionIfOccured
                    + ", defaultValue=" + defaultValue
            );
        }
        Setting setting = null;
        try {
            setting = eatlogDsl.selectFrom(SETTING)
                    .where(SETTING.KEY.eq(key))
                    .fetchOneInto(Setting.class);
        } catch (Throwable e) {
            log.warn("Failed fetch Setting by key=" + key
                    + (throwExceptionIfOccured ? "" : ", using defaultValue='" + defaultValue + "'"), e);
            if(throwExceptionIfOccured)
                throw e;
        }
        if(setting == null) {
            return defaultValue;
        }
        if(!convertJsonValueToMap) {
            return setting;
        }
        JSONParser parser = new JSONParser();
        try {
            /* Map<?, ?> for json */
            return parser.parse(setting.getValue(), new DefaultContainerFactory());
        } catch (Throwable e) {
            log.warn("Failed convert setting value from json to Map"
                    + ", setting=" + setting
                    + ", using defaultValue='" + defaultValue + "'", e);
            return defaultValue;
        }
    }

    @Cacheable(cacheNames = observedAccountByIdCache, key = "#id",
            condition = "#id != null", sync = true)
    public Account allocateObservedAccountById(UInteger id) {
        if(log.isDebugEnabled())
            log.debug("\nallocateObservedAccountById id=" + id);
        Account account = null;
        try {
            account = eatlogDsl.select(ACCOUNT.ID, ACCOUNT.NAME)
                    .from(ACCOUNT)
                    .where(ACCOUNT.ID.eq(id))
                    .fetchOneInto(Account.class);
        } catch (Throwable e) {
            log.warn("Failed fetch Account for observation by accountId=" + id, e);
        }
        return account;
    }
}