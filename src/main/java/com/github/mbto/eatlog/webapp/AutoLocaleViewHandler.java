package com.github.mbto.eatlog.webapp;

import com.github.mbto.eatlog.common.dto.SiteVisitor;
import com.github.mbto.eatlog.service.ApplicationHolder;
import com.github.mbto.eatlog.webapp.session.SessionAccount;
import com.sun.faces.application.view.MultiViewHandler;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * https://stackoverflow.com/a/12094595
 * https://primefaces.github.io/primefaces/12_0_0/#/core/localization
 * https://memorynotfound.com/jsf-pass-parameters-localized-error-messages/
 * https://docs.joinfaces.org/5.0.5/reference/#_internationalization_and_resources_bundles
 * https://mkyong.com/jsf2/jsf-2-internationalization-example/
 * https://mkyong.com/java/java-convert-chinese-character-to-unicode-with-native2ascii/
 * https://stackoverflow.com/questions/2668161/internationalization-in-jsf-when-to-use-message-bundle-and-resource-bundle/2668602#2668602
 * https://stackoverflow.com/questions/4830588/localization-in-jsf-how-to-remember-selected-locale-per-session-instead-of-per
 * https://github.com/primefaces/primefaces/blob/master/primefaces/src/main/resources/org/primefaces/Messages_ru.properties
 */
@Slf4j
@SuppressWarnings("JavadocLinkAsPlainText")
public class AutoLocaleViewHandler extends MultiViewHandler {
    private ApplicationHolder applicationHolder;

    {
        log.debug("\nAutoLocaleViewHandler object initialization");
    }

    @Override
    public Locale calculateLocale(FacesContext fc) {
        if(applicationHolder == null) {
            applicationHolder = fc.getApplication()
                    .evaluateExpressionGet(fc, "#{applicationHolder}", ApplicationHolder.class);
        }
        HttpSession session = (HttpSession) fc.getExternalContext()
                .getSession(false);
        if (session != null) {
            SiteVisitor siteVisitor = applicationHolder.getSiteVisitorBySessionId()
                    .get(session.getId());
            if(siteVisitor != null) {
                siteVisitor.updateExpiredDateTime();
            }
            SessionAccount sessionAccount = (SessionAccount) session.getAttribute("sessionAccount");
            if (sessionAccount != null) {
                Locale locale = sessionAccount.getLocale();
                if (locale != null) {
                    return locale;
                }
            }
        }
        return applicationHolder.determineAvailableLocale(fc.getExternalContext()
                .getRequestLocale());
    }
}