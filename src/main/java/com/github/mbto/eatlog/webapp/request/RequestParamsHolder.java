package com.github.mbto.eatlog.webapp.request;

import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.custommodel.ObservationWrapper;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account;
import com.github.mbto.eatlog.service.CacheService;
import com.github.mbto.eatlog.webapp.session.SessionAccount;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.types.UInteger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.utils.ProjectUtils.addDotsForBigString;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;
import static jakarta.faces.application.FacesMessage.*;

/**
 * Don't remove @RequestScoped annotation, we can use this
 * component in .xhtml templates and @ViewScoped components
 */
@RequestScoped
@Named
@Slf4j
public class RequestParamsHolder {
    @Autowired
    private CacheService cacheService;
    @Autowired
    private SessionAccount sessionAccount;
    @Getter
    private String actualRequestParamsMerged;
    @Getter
    private ObservationWrapper observationWrapper;
    @Getter
    private boolean showGoToYourPageLink;

    public void onInvokeApplication() {
        onInvokeApplication(true, (String[]) null);
    }

    public void onInvokeApplication(boolean isObservationPossible) {
        onInvokeApplication(isObservationPossible, (String[]) null);
    }

    public void onInvokeApplication(boolean isObservationPossible, String... requiredRolesNames) {
        onInvokeApplicationInternal(isObservationPossible, requiredRolesNames);
        defineObservationWrapper(null);
    }

    private void onInvokeApplicationInternal(boolean isObservationPossible, String... requiredRolesNames) {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled)
            log.debug("\nonInvokeApplicationInternal isObservationPossible=" + isObservationPossible
                    + ", requiredRolesNames=" + Arrays.toString(requiredRolesNames));
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        Map<String, String> requestParamValueByKey = ec.getRequestParameterMap();
        if(!requestParamValueByKey.isEmpty()) {
            actualRequestParamsMerged = CARRY_REQUEST_PARAMS.stream()
                    .filter(requestParamValueByKey::containsKey)
                    .map(paramKey -> {
                        String paramValue = requestParamValueByKey.get(paramKey);
                        if (StringUtils.isBlank(paramValue)) {
                            return paramKey;
                        }
                        int qPos = paramValue.indexOf("?"); // case for replace coordinates x,y with p:graphicImage in header avatar image
                        if(qPos > -1) {
                            paramValue = paramValue.substring(0, qPos);
                        }
                        return paramKey + "=" + paramValue;
                    }).collect(Collectors.joining("&", "?", ""));
            if(isDebugEnabled)
                log.debug("\nonInvokeApplicationInternal result actualRequestParamsMerged=" + actualRequestParamsMerged);
        }
        if(!sessionAccount.isAuthed()) {
            fc.addMessage(userStateMsgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("auth.in.account.page"), ""));
            if(isDebugEnabled)
                log.debug("\nonInvokeApplicationInternal result auth.in.account.page");
            return;
        }
        InternalAccount internalAccount = sessionAccount.getInternalAccount();
        if(internalAccount.getIsBanned()) {
            fc.addMessage(userStateMsgs, new FacesMessage(SEVERITY_ERROR,
                    msgFromBundle("you.are.banned"),
                    internalAccount.getBannedReason()));
            if(isDebugEnabled)
                log.debug("\nonInvokeApplicationInternal result you.are.banned");
            return;
        }
        if(!internalAccount.getCalcedRoles().isOwner() && requiredRolesNames != null) {
            for (String roleName : requiredRolesNames) {
                if (!internalAccount.hasRole(roleName)) {
                    fc.addMessage(userStateMsgs, new FacesMessage(SEVERITY_ERROR,
                            msgFromBundle("role.required.for.view", roleName), ""));
                    if(isDebugEnabled)
                        log.debug("\nonInvokeApplicationInternal role.required.for.view roleName=" + roleName);
                }
            }
        }
        if(!isObservationPossible) {
            if(isDebugEnabled)
                log.debug("\nonInvokeApplicationInternal result isObservationPossible=" + isObservationPossible);
            return;
        }
        String observedAccountIdStr = requestParamValueByKey.get(OBSERVE_USER_REQUEST_PARAM);
        if(observedAccountIdStr == null) {
            if(isDebugEnabled)
                log.debug("\nonInvokeApplicationInternal result observedAccountIdStr=" + observedAccountIdStr);
            return;
        }
        UInteger tmpObservedAccountId;
        try {
            tmpObservedAccountId = UInteger.valueOf(observedAccountIdStr);
        } catch (Throwable ignored) {
            fc.addMessage(userStateMsgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("view.user.page.canceled"),
                    msgFromBundle("invalid.value.of.param",
                            addDotsForBigString(observedAccountIdStr, 12),
                            OBSERVE_USER_REQUEST_PARAM)));
            showGoToYourPageLink = true;
            if(isDebugEnabled) {
                log.debug("\nonInvokeApplicationInternal result view.user.page.canceled, invalid.value.of.param"
                        + " observedAccountIdStr=" + observedAccountIdStr
                        + ", OBSERVE_USER_REQUEST_PARAM=" + OBSERVE_USER_REQUEST_PARAM);
            }
            return;
        }
        Account observedAccount = cacheService.allocateObservedAccountById(tmpObservedAccountId);
        if(observedAccount == null) {
            fc.addMessage(userStateMsgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("view.user.page.canceled"),
                    msgFromBundle("user.not.founded", tmpObservedAccountId)));
            showGoToYourPageLink = true;
            if(isDebugEnabled)
                log.debug("\nonInvokeApplicationInternal result view.user.page.canceled, user.not.founded tmpObservedAccountId=" + tmpObservedAccountId);
            return;
        }
        if(!isObservedEqualsSessionAccount(observedAccount)) {
            fc.addMessage(userStateMsgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("view.user.page"),
                    observedAccount.getName()));
            showGoToYourPageLink = true;
        }
        defineObservationWrapper(observedAccount);
        if(isDebugEnabled) {
            log.debug("\nonInvokeApplicationInternal result view.user.page, observedAccount=" + observedAccount);
        }
    }

    private void defineObservationWrapper(Account observedAccount) {
        if(!(observationWrapper == null && sessionAccount.isAuthed()))
            return;
        boolean isDebugEnabled = log.isDebugEnabled();
        if(isDebugEnabled)
            log.debug("\ndefineObservationWrapper observedAccount=" + observedAccount);
        observationWrapper = observedAccount != null && !isObservedEqualsSessionAccount(observedAccount)
                ? new ObservationWrapper(observedAccount, true)
                : new ObservationWrapper(sessionAccount.getInternalAccount(), false);
        if(isDebugEnabled)
            log.debug("\ndefineObservationWrapper result observationWrapper=" + observationWrapper);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isObservedEqualsSessionAccount(Account observedAccount) {
        return sessionAccount.getInternalAccount().getId().equals(observedAccount.getId());
    }
}