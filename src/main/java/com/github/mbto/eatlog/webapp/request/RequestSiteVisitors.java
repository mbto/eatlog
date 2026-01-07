package com.github.mbto.eatlog.webapp.request;

import com.github.mbto.eatlog.common.dto.SiteVisitors;
import com.github.mbto.eatlog.service.CacheService;
import com.github.mbto.eatlog.webapp.WebUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.mbto.eatlog.utils.ProjectUtils.declensionValuedL10N;

/**
 * We can use #{cacheService.allocateSettingByKey()} directly in .xhtml templates,
 * but it's more convenient to use RequestSetting component with selectedChild
 */
@RequestScoped
@Named
@Slf4j
public class RequestSiteVisitors {
    @Autowired
    private CacheService cacheService;

    @Getter
    private SiteVisitors siteVisitors;

    @Getter
    private String online;

    @PostConstruct
    public void fetch() {
        siteVisitors = cacheService.allocateSiteVisitors();
        long guests = siteVisitors.getGuestsCount();
        long users = siteVisitors.getAuthedUsers().size();
        String onSite = WebUtils.msgFromBundle("on.site");
        String and = WebUtils.msgFromBundle("and");
        StringBuilder sb = new StringBuilder(onSite).append(' ');
        boolean hasGuests = guests > 0;
        boolean hasUsers = users > 0;
        if (hasGuests) {
            sb.append(declensionValuedL10N(guests, "guest"));
        }
        if (hasGuests && hasUsers) {
            sb.append(' ').append(and).append(' ');
        }
        if (hasUsers) {
            sb.append(declensionValuedL10N(users, "user")).append(": ");
        }
        online = sb.toString();
    }
}