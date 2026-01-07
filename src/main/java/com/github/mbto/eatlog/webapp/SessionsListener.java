package com.github.mbto.eatlog.webapp;

import com.github.mbto.eatlog.common.dto.SiteVisitor;
import com.github.mbto.eatlog.service.ApplicationHolder;
import com.github.mbto.eatlog.service.CacheService;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.mbto.eatlog.service.CacheService.allocateSiteVisitorsKey;
import static com.github.mbto.eatlog.service.CacheService.siteVisitorsCache;

@Component
@WebListener
public class SessionsListener implements HttpSessionListener {
    @Autowired
    private CacheService cacheService;
    @Autowired
    private ApplicationHolder applicationHolder;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        Object sessionObject = se.getSource();
        if(sessionObject instanceof HttpSession httpSession) {
            String sessionId = httpSession.getId();
            if(sessionId != null) {
                AtomicBoolean created = new AtomicBoolean(false);
                applicationHolder.getSiteVisitorBySessionId().computeIfAbsent(sessionId, s -> {
                    created.set(true);
                    return new SiteVisitor();
                });
                if(created.get()) {
                    cacheService.evict(siteVisitorsCache, allocateSiteVisitorsKey, false);
                    // for resort siteVisitorsOutputPanelId after removed old siteVisitor
                }
            }
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        Object sessionObject = se.getSource();
        if(sessionObject instanceof HttpSession httpSession) {
            String sessionId = httpSession.getId();
            if(sessionId != null) {
                SiteVisitor siteVisitor = applicationHolder.getSiteVisitorBySessionId().remove(sessionId);
                if(siteVisitor != null) {
                    siteVisitor.clearFields();
                    cacheService.evict(siteVisitorsCache, allocateSiteVisitorsKey, false);
                    // for resort siteVisitorsOutputPanelId after removed old siteVisitor
                }
            }
        }
    }
}