package com.github.mbto.eatlog.webapp.request;

import com.github.mbto.eatlog.service.CacheService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * We can use #{cacheService.allocateSettingByKey()} directly in .xhtml templates,
 * but it's more convenient to use RequestSetting component with selectedChild
 */
@RequestScoped
@Named
@Slf4j
public class RequestSetting {
    @Autowired
    private CacheService cacheService;

    @Getter
    private Object selectedChild;

    public void fetch(String key,
                      boolean convertJsonValueToMap,
                      boolean throwExceptionIfOccured,
                      String defaultValue) {
        if (log.isDebugEnabled())
            log.debug("\nfetch");
        selectedChild = cacheService.allocateSettingByKey(key, convertJsonValueToMap, throwExceptionIfOccured, defaultValue);
    }
}