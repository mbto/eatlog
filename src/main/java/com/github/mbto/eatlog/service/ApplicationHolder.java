package com.github.mbto.eatlog.service;

import com.github.mbto.eatlog.common.dto.LocaleSettings;
import com.github.mbto.eatlog.common.dto.SiteVisitor;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.primefaces.application.exceptionhandler.ExceptionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.github.mbto.eatlog.common.Constants.GEO_SCHEMA_NAME;
import static com.github.mbto.eatlog.common.Constants.GEO_TABLES_NAMES;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.collectResources;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.declensionValued;

@Service
@Lazy(false)
@Slf4j
public class ApplicationHolder {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private DSLContext eatlogDsl;

    @Getter
    private boolean isDevEnvironment;
    @Getter
    private boolean isTestEnvironment;
    @Getter
    private Map<String, String> queryByFilename;
    @Getter
    private Map<Locale, LocaleSettings> localeSettingsByAvailableLocale;
    private Map<String, Map.Entry<Locale, LocaleSettings>> alternativeLocaleEntryByLanguage;
    @Getter
    private final Map<String, SiteVisitor> siteVisitorBySessionId = new ConcurrentHashMap<>(512);

    @PostConstruct
    public void init() {
        List<String> profiles = Arrays.asList(applicationContext
                .getEnvironment()
                .getActiveProfiles());
        isDevEnvironment = profiles.contains("dev");
        isTestEnvironment = profiles.contains("test");
        reloadQueries();
        reloadLocales();
        try {
            isGeoInfoAvailable(eatlogDsl, true);
        } catch (Throwable e) {
            log.warn("Failed check availability GeoInfo fetching", e);
        }
    }

    public void reloadQueries() {
        queryByFilename = collectResources("queries", "sql");
        log.info("Reloaded " + declensionValued(queryByFilename.size(), "sql quer", "y", "ies", "ies"));
    }

    public void reloadLocales() {
        Map<String, String> payloadByL10NBundleFilename = collectResources("l10n", "properties");
        String L10NBundleName = "Messages_";
        localeSettingsByAvailableLocale = new LinkedHashMap<>();
        alternativeLocaleEntryByLanguage = new HashMap<>();
        for (Map.Entry<String, String> entry : payloadByL10NBundleFilename.entrySet()) {
            String filename = entry.getKey();
            int pos = filename.indexOf(L10NBundleName);
            if(pos == -1)
                continue;
            String payload = entry.getValue();
            String segments = filename.substring(pos + L10NBundleName.length());
            Properties properties = new Properties();
            try(StringReader sr = new StringReader(payload)) {
                properties.load(sr);
            } catch (Throwable e) {
                throw new RuntimeException("Failed load properties from payload"
                        + ", L10N bundle filename=" + filename, e);
            }
            int order = Integer.parseInt(String.valueOf(properties.getOrDefault("language.order", "0")));
            String label = String.valueOf(properties.getOrDefault("language.label", segments));
            Locale availableLocale;
            try {
                availableLocale = LocaleUtils.toLocale(segments);
                if(!LocaleUtils.isAvailableLocale(availableLocale))
                    throw new RuntimeException("Failed validation locale '" + availableLocale + "', due not available");
            } catch (Throwable e) {
                throw new RuntimeException("Unable to determine availableLocale with"
                        + " segments=" + segments
                        + " from L10N bundle filename=" + filename, e);
            }
            LocaleSettings localeSettings = new LocaleSettings(order, label);
            if(localeSettingsByAvailableLocale.put(availableLocale, localeSettings) != null) {
                throw new IllegalStateException("Failed put"
                        + " availableLocale=" + availableLocale
                        + " with localeSettings=" + localeSettings
                        + " segments=" + segments
                        + " from L10N bundle filename=" + filename
                        + ", due already exists");
            }
            localeSettings.setAlternativeLocales(
                    new HashSet<>(LocaleUtils.countriesByLanguage(availableLocale.getLanguage())));
            alternativeLocaleEntryByLanguage.put(availableLocale.getLanguage(),
                    Map.entry(availableLocale, localeSettings));
        }
        if(localeSettingsByAvailableLocale.isEmpty()) {
            throw new IllegalStateException("L10N bundles with name '" + L10NBundleName + "*' required");
        }
        localeSettingsByAvailableLocale = localeSettingsByAvailableLocale.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().getOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (lw1, lw2) -> {
                            throw new IllegalStateException("Dublicate keys founded with lw1=" + lw1 + ", lw2=" + lw2);
                        }, LinkedHashMap::new));
        log.info("Available locales: " + localeSettingsByAvailableLocale
                + " from L10N bundles: " + payloadByL10NBundleFilename.keySet());
    }

    public Locale determineAvailableLocale(Locale locale) {
        if(locale == null) {
            return localeSettingsByAvailableLocale.keySet().iterator().next();
        }
        if(localeSettingsByAvailableLocale.containsKey(locale)) {
            return locale;
        }
        Map.Entry<Locale, LocaleSettings> entry = alternativeLocaleEntryByLanguage.get(locale.getLanguage());
        if(entry != null && entry.getValue()
                .getAlternativeLocales()
                .contains(locale)) {
            Locale alternativeLocale = entry.getKey();
            if(localeSettingsByAvailableLocale.containsKey(alternativeLocale))
                return alternativeLocale;
        }
        return localeSettingsByAvailableLocale.keySet().iterator().next();
    }

    public void clearPfExceptionHandler() {
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.getExternalContext().getSessionMap().remove(ExceptionInfo.ATTRIBUTE_NAME);
    }

    public boolean isGeoInfoAvailable(DSLContext dslContext, boolean isLogEnabled) throws Exception {
        Field<String> tableNameField = DSL.field("TABLE_NAME", String.class);
        List<String> tableNamesSlice = dslContext.select(tableNameField)
                .from(DSL.table("information_schema.TABLES"))
                .where(DSL.field("TABLE_SCHEMA").eq(GEO_SCHEMA_NAME),
                        DSL.field("TABLE_NAME").in(GEO_TABLES_NAMES))
                .orderBy(DSL.one())
                .fetchInto(tableNameField.getType());
        if(tableNamesSlice.size() != GEO_TABLES_NAMES.size()) {
            if(isLogEnabled) {
                log.warn("Disabled fetching GeoInfo, some tables of database `" + GEO_SCHEMA_NAME + "`"
                        + " required=" + GEO_TABLES_NAMES
                        + ", founded=" + tableNamesSlice
                        + ". Eatlog uses geo tables from https://github.com/mbto/maxmind-geoip2-csv2sql-converter"
                        + " with config 'GeoLite2-City-CSV.mysql.eatlog.ini' in project root"
                );
            }
            return false;
        }
        if(isLogEnabled) {
            log.info("Available fetching GeoInfo");
        }
        return true;
    }

    public void testThrowRuntimeException() {
        throw new RuntimeException("test RuntimeException");
    }

    public void testThrowError() {
        throw new Error("test Error");
    }

    public void testDebugMe() {
        log.info("testDebugMe");
    }
}