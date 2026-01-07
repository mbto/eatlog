package com.github.mbto.eatlog.webapp.request.auth;

import com.github.jgonian.ipmath.Ipv4;
import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.dto.auth.YandexAuth;
import com.github.mbto.eatlog.service.AccountService;
import com.github.mbto.eatlog.service.auth.YandexOAuthService;
import com.github.mbto.eatlog.webapp.WebUtils;
import com.github.mbto.eatlog.webapp.session.SessionAccount;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.jooq.types.UInteger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.webapp.WebUtils.getBaseUrl;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;

/*
https://oauth.yandex.ru/
*/
@RequestScoped
@Named
@Slf4j
public class RequestLogin {
    @Autowired
    private YandexOAuthService yandexOAuthService;

    @Autowired
    private AccountService accountService;
    @Autowired
    private SessionAccount sessionAccount;

    public void redirectToOAuth20Provider() {
        if (log.isDebugEnabled())
            log.debug("\nredirectToOAuth20Provider");
        FacesContext fc = FacesContext.getCurrentInstance();
        String state = UUID.randomUUID().toString();
        sessionAccount.setAuthState(state);
        String authorizationUrl;
        try {
            authorizationUrl = yandexOAuthService.buildLoginUrl(state);
        } catch (Throwable e) {
            log.warn("Failed build authorizationUrl", e);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.build.authorizationUrl"),
                    e.toString())
            );
            return;
        }
        log.info("Builded redirect authorizationUrl=" + authorizationUrl);
        WebUtils.sendRedirect(authorizationUrl);
    }

/* Accept OAuth20 params
 * http://127.0.0.1:8097/account
 *      ?state=vJueb1mMRDElJI4N9ymJRUFvZkns2PGelU71Bclnnmo%3D
 *      &code=rn2jldi7dthhb5br
 *      &cid=yppj1h7y73tmc6c29rqxp3594r
 * */
    public void onInvokeApplication() {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled)
            log.debug("\nonInvokeApplication");
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> requestParamValueByKey = fc.getExternalContext().getRequestParameterMap();
        if(requestParamValueByKey.containsKey(LOGOUT_REQUEST_PARAM)) {
            if(sessionAccount.isAuthed()) {
                fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("you.logged.out"), "")
                );
                detachAccountAndInvalidateSession();
            }
            return;
        }
        String state = requestParamValueByKey.get("state");
        String authState = sessionAccount.getAuthState();
        if(authState == null || !authState.equals(state)) {
            return;
        }
        sessionAccount.setAuthState(null);
        String code = requestParamValueByKey.get("code");
        if(code == null) {
            return;
        }
        log.info("Try auth, requestParamValueByKey=" + requestParamValueByKey);
        YandexAuth auth;
        try {
            auth = yandexOAuthService.fetchUserProfile(code, getBaseUrl() + ACCOUNT_PAGE);
        } catch (Throwable e) {
            log.warn("Failed auth, requestParamValueByKey=" + requestParamValueByKey, e);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.auth"), e.toString())
            );
            return;
        }
        String ipv4 = WebUtils.extractIpFromFacesContext();
        UInteger ip = UInteger.valueOf(Ipv4.of(ipv4).asBigInteger().longValue());
        InternalAccount internalAccount = accountService.findOrCreateAccount(
                auth,
                sessionAccount.getLocale().toString(),
                ip
        );
        sessionAccount.attachAccount(internalAccount);
        WebUtils.sendRedirectInternal("account?id=" + internalAccount.getId());
    }

    private void detachAccountAndInvalidateSession() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        // evictSiteVisitorsCache=false, due after ec.invalidateSession()
        // SessionsListener::sessionDestroyed() will be invoked, with cache eviction
        sessionAccount.detachAccount(false);
        try {
            ec.invalidateSession();
        } catch (Throwable ignored) {}
    }
}