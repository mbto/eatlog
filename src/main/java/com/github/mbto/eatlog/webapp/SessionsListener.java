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

import static com.github.mbto.eatlog.service.CacheService.allocateSiteVisitorsKey;
import static com.github.mbto.eatlog.service.CacheService.hoursCache;

@Component
@WebListener
public class SessionsListener implements HttpSessionListener {
    @Autowired
    private CacheService cacheService;
    @Autowired
    private ApplicationHolder applicationHolder;

//    @Override
//    public void sessionCreated(HttpSessionEvent se) {
//        Object sessionObject = se.getSource();
//        if(sessionObject instanceof HttpSession httpSession) {
//            if(!applicationHolder.isDevEnvironment())
//                return;
//            for (int i = 0, rand = java.util.concurrent
//                    .ThreadLocalRandom
//                    .current()
//                    .nextInt(10,20);
//                 i < rand; i++) {
//                com.github.mbto.eatlog.common.dto.SiteVisitor siteVisitor = new com.github.mbto.eatlog.common.dto.SiteVisitor();
//                siteVisitor.setAccountId(org.jooq.types.UInteger.valueOf(i));
//                int[] geonameIds = {
//                        6535294,6535635,8975538,8953157,8950614,8948793,3168348,3174496,3171251,3172916,3172873,
//                        3165771,3172641,3172377,3172376,3167215,3171874,3171783,3171446,3166083,3175714,3176082,
//                        5601538,5446028,5202009,4642988,524901};
//                int geonameId = geonameIds[java.util.concurrent.ThreadLocalRandom.current().nextInt(0, geonameIds.length)];
//                siteVisitor.setName(geonameId + ", " + java.util.UUID.randomUUID().toString().substring(0,
//                        java.util.concurrent.ThreadLocalRandom.current().nextInt(3, 10)));
//                siteVisitor.setGeoInfo(cacheService.allocateGeoInfoByGeonameId(org.jooq.types.UInteger.valueOf(geonameId)));
//                applicationHolder.getSiteVisitorBySessionId().put(siteVisitor.getName(), siteVisitor);
//            }
//            cacheService.evict(hoursCache, allocateSiteVisitorsKey); // for resort siteVisitorsOutputPanelId after removed old siteVisitor
//        }
//    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        Object sessionObject = se.getSource();
        if(sessionObject instanceof HttpSession httpSession) {
            String sessionId = httpSession.getId();
            if(sessionId != null) {
                SiteVisitor siteVisitor = applicationHolder.getSiteVisitorBySessionId().remove(sessionId);
                if(siteVisitor != null) {
                    siteVisitor.clearFields();
                    cacheService.evict(hoursCache, allocateSiteVisitorsKey, false); // for resort siteVisitorsOutputPanelId after removed old siteVisitor
                }
            }
        }
    }
}