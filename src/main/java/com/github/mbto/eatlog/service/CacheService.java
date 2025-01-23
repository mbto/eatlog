package com.github.mbto.eatlog.service;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.mbto.eatlog.common.dto.GeoInfo;
import com.github.mbto.eatlog.common.dto.SiteVisitor;
import com.github.mbto.eatlog.common.dto.SiteVisitors;
import com.github.mbto.eatlog.common.mapper.GeoInfoMapper;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Setting;
import com.github.mbto.eatlog.common.utils.DefaultContainerFactory;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.tools.json.JSONParser;
import org.jooq.types.UInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Account.ACCOUNT;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Setting.SETTING;

@SuppressWarnings("UnusedReturnValue")
@Service
@Lazy(false)
@Slf4j
public class CacheService {
    public static final String geoInfoByGeonameIdCache = "geoInfoByGeonameIdCache";
    public static final String geonameIdByIpCache = "geonameIdByIpCache";
    public static final String objectBySettingKeyCache = "objectBySettingKeyCache";
    public static final String observedAccountByIdCache = "observedAccountByIdCache";
    public static final String hoursCache = "hoursCache";
    public static final String allocateSiteVisitorsKey = "allocateSiteVisitors";

    @Autowired
    private ApplicationHolder applicationHolder;
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private CacheManager cacheManager;

    @Value("${eatlog.datasource.fetchForCache.queryTimeoutSec:#{100}}")
    private int queryTimeout;
    @Value("${eatlog.datasource.fetchForCache.durationExceedingLimitForLogMillis:#{1000}}")
    private int durationExceedingLimitForLogMillis;

    @Cacheable(cacheNames = hoursCache, key = "#root.methodName", sync = true)
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

    @Cacheable(cacheNames = geoInfoByGeonameIdCache, key = "#geonameId",
            condition = "#geonameId != null", sync = true)
    public GeoInfo allocateGeoInfoByGeonameId(UInteger geonameId) {
        if(log.isDebugEnabled())
            log.debug("\nallocateGeoInfoByGeonameId geonameId=" + geonameId);
        Set<UInteger> unresolvedGeonameIds = new HashSet<>(1, 1f);
        unresolvedGeonameIds.add(geonameId);
        Map<UInteger, GeoInfo> geoInfoByGeonameId = getGeoInfoByGeonameIdMap(unresolvedGeonameIds, false);
        if(CollectionUtils.isEmpty(geoInfoByGeonameId))
            return null;
        return geoInfoByGeonameId.get(geonameId);
    }

    public Map<UInteger, GeoInfo> getGeoInfoByGeonameIdMap(Set<UInteger> geonameIds, boolean cacheResults) {
        if(log.isDebugEnabled())
            log.debug("\npreCacheGeoInfosByGeonameIds geonameIds=" + geonameIds + ", cacheResults=" + cacheResults);
        if(CollectionUtils.isEmpty(geonameIds))
            return null;
        long fetchStartedAt = System.currentTimeMillis();
        Map<UInteger, GeoInfo> geoInfoByGeonameId = null;
        try {
            geoInfoByGeonameId = eatlogDsl.transactionResult(config -> {
                DSLContext transactionalDsl = DSL.using(config);
                if (!applicationHolder.isGeoInfoAvailable(transactionalDsl, log.isDebugEnabled())) {
                    return null;
                }
                try {
                    transactionalDsl.execute(LOCK_GEO_TABLES_STATEMENT);
                    Map<String, String> queryByFilename = applicationHolder.getQueryByFilename();
                    return transactionalDsl
                            .resultQuery(queryByFilename.get("geoinfo-by-geoname_ids"),
                                    DSL.table(GEO_SCHEMA_NAME),
                                    DSL.list(geonameIds.stream()
                                            .map(DSL::val)
                                            .toList()))
                            .queryTimeout(queryTimeout)
                            .fetchMap(r -> r.get("geoname_id", UInteger.class),
                                    new GeoInfoMapper(applicationHolder
                                            .getLocaleSettingsByAvailableLocale()
                                            .keySet()));
                } finally {
                    try {
                        transactionalDsl.execute(UNLOCK_TABLES_STATEMENT);
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable e) {
            log.warn("Failed fetch GeoInfo by geonameIds=" + geonameIds, e);
        }
        long fetchDuration = System.currentTimeMillis() - fetchStartedAt;
        if(fetchDuration >= durationExceedingLimitForLogMillis) {
            log.warn("Slow fetching GeoInfo by geonameIds=" + geonameIds
                    + ", fetchDuration=" + fetchDuration + "ms");
        }
        if(!cacheResults)
            return geoInfoByGeonameId;
        CaffeineCache cache = getCache(geoInfoByGeonameIdCache);
        if(!CollectionUtils.isEmpty(geoInfoByGeonameId)) {
            for (Map.Entry<UInteger, GeoInfo> entry : geoInfoByGeonameId.entrySet()) {
                UInteger geonameId = entry.getKey();
                cache.put(geonameId, entry.getValue());
                geonameIds.remove(geonameId);
            }
        }
        for (UInteger geonameId : geonameIds) {
            if(cache.get(geonameId) == null) {
                cache.put(geonameId, null);
            }
        }
        return geoInfoByGeonameId;
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

    @Cacheable(cacheNames = geonameIdByIpCache, key = "#ip",
            condition = "#ip != null", sync = true)
    public UInteger allocateGeonameIdByIp(String ip) {
        if(log.isDebugEnabled())
            log.debug("\nallocateGeonameIdByIp ip=" + ip);
        long fetchStartedAt = System.currentTimeMillis();
        GeoInfo geoInfo;
        try {
            geoInfo = eatlogDsl.transactionResult(config -> {
                DSLContext transactionalDsl = DSL.using(config);
                if(!applicationHolder.isGeoInfoAvailable(transactionalDsl, log.isDebugEnabled())) {
                    return null;
                }
                try {
                    transactionalDsl.execute(LOCK_GEO_TABLES_STATEMENT);
                    Map<String, String> queryByFilename = applicationHolder.getQueryByFilename();
                    return transactionalDsl
                            .resultQuery(queryByFilename.get("geoinfo-by-ip"),
                                    DSL.table(GEO_SCHEMA_NAME),
                                    DSL.val(ip))
                            .queryTimeout(queryTimeout)
                            .fetchOne(new GeoInfoMapper(applicationHolder
                                    .getLocaleSettingsByAvailableLocale()
                                    .keySet()));
                } finally {
                    try {
                        transactionalDsl.execute(UNLOCK_TABLES_STATEMENT);
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable e) {
            long fetchDuration = System.currentTimeMillis() - fetchStartedAt;
            log.warn("Failed fetch GeoInfo by ip=" + ip
                    + ", fetchDuration=" + fetchDuration + "ms", e);
            return null;
        }
        long fetchDuration = System.currentTimeMillis() - fetchStartedAt;
        if(fetchDuration >= durationExceedingLimitForLogMillis) {
            log.warn("Slow fetching GeoInfo by ip=" + ip
                    + ", fetchDuration=" + fetchDuration + "ms");
        }
        if(geoInfo != null) {
            UInteger geonameId = geoInfo.getGeonameId();
            getCache(geoInfoByGeonameIdCache)
                    .putIfAbsent(geonameId, geoInfo);
            return geonameId;
        }
        return null;
    }

    @Cacheable(cacheNames = objectBySettingKeyCache, key = "#key",
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