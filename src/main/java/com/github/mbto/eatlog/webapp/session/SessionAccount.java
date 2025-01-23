package com.github.mbto.eatlog.webapp.session;

import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.dto.SiteVisitor;
import com.github.mbto.eatlog.service.ApplicationHolder;
import com.github.mbto.eatlog.service.CacheService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.mbto.eatlog.common.Constants.msgs;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Account.ACCOUNT;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.*;
import static com.github.mbto.eatlog.service.CacheService.*;
import static com.github.mbto.eatlog.webapp.WebUtils.*;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;

@SessionScoped
@Named
@Slf4j
public class SessionAccount implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Autowired
    private DSLContext eatlogDsl;
//    @Autowired
//    private AccountService accountService; // for autologin while develop
    @Autowired
    private CacheService cacheService;
    @Autowired
    private ApplicationHolder applicationHolder;

    private Integer authStateValue;
    @Getter
    private InternalAccount internalAccount;
    @Getter
    private Locale locale;
    @Getter
    private UInteger geonameId;

    private int localChangesCounter;

    @PostConstruct
    public void init() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if(fc.isPostback())
            return;
        if(log.isDebugEnabled())
            log.debug("\ninit");
        if(!isAuthed()) {
            Locale newLocale = fc.getViewRoot().getLocale();
            if(newLocale == null)
                newLocale = fc.getExternalContext().getRequestLocale();
            setLocale(newLocale);
        } else {
            setLocale(internalAccount.getLocaleSegments());
        }
        onLocaleChanged();
        defineGeoInfoByIp();
        String sessionId = extractSessionId();
        if(sessionId != null) {
            SiteVisitor siteVisitor = new SiteVisitor();
            applicationHolder.getSiteVisitorBySessionId().put(sessionId, siteVisitor);
            cacheService.evict(hoursCache, allocateSiteVisitorsKey, false); // for resort siteVisitorsOutputPanelId after added new siteVisitor
        }
//      for autologin while develop
//        if(!fc.getExternalContext().getRequestParameterMap().containsKey(LOGOUT_REQUEST_PARAM)) {
//            attachAccount(accountService.findOrCreateAccount(
//                    new GoogleAuth("108555958097693113620"),
//                    locale.toString(), geonameId));
//        }
    }

    public void setLocale(String localeSegments) {
        if(log.isDebugEnabled())
            log.debug("\nsetLocale localeSegments=" + localeSegments);
        Locale locale = null;
        boolean exceptionOccured = false;
        try {
            locale = LocaleUtils.toLocale(localeSegments);
        } catch (Throwable ignored) {
            exceptionOccured = true;
        }
        if(!exceptionOccured && LocaleUtils.isAvailableLocale(locale)) {
            setLocale(locale);
            return;
        }
        setLocale((Locale) null);
    }

    public void setLocale(Locale newLocale) {
        locale = applicationHolder.determineAvailableLocale(newLocale);
        if(log.isDebugEnabled())
            log.debug("\nsetLocale result locale=" + locale);
    }

    public void onLocaleChanged() {
        if(log.isDebugEnabled())
            log.debug("\nonLocaleChanged locale=" + locale);
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.getViewRoot().setLocale(locale);
        if(!isAuthed()) {
            return;
        }
        UInteger accountId = internalAccount.getId();
        localChangesCounter = 0;
        try {
            //noinspection ArraysAsListWithZeroOrOneArgument
            localChangesCounter = pointwiseUpdateQuery(eatlogDsl, ACCOUNT.ID, accountId,
                    Arrays.asList(
                            Pair.of(ACCOUNT.LOCALE_SEGMENTS, locale.toString())
                    ));
        } catch (Throwable e) {
            log.warn("Failed update locale='" + locale + "' for accountId=" + accountId, e);
        }
    }

    public void defineGeoInfoByIp() {
        boolean isDebugEnabled = log.isDebugEnabled();
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        HttpServletRequest req = (HttpServletRequest) ec.getRequest();
        String ip = ec.getRequestHeaderMap().get("X-Real-IP");
        if(StringUtils.isBlank(ip))
            ip = req.getRemoteAddr();
        if(isDebugEnabled) {
            log.debug("\ndefineGeoInfo ip=" + ip);
//            List<UInteger> existedGeonameIds = Stream.of(
//                    466343, 2971357, 466363, 6822202,
//                    3690654, 591876, 2971350, 323716,
//                    769924, 1485712, 0
//            ).map(UInteger::valueOf).toList();
//            geonameId = existedGeonameIds
//                    .get(ThreadLocalRandom.current()
//                            .nextInt(0, existedGeonameIds.size()));
//            if(UInteger.MIN.equals(geonameId))
//                geonameId = null;
        }
        geonameId = cacheService.allocateGeonameIdByIp(ip);
        if(isDebugEnabled) {
            log.debug("\ndefineGeoInfo ip=" + ip + ", result=" + geonameId);
        }
    }
    public void updateName() {
        String name = internalAccount.getName();
        String newName = StringUtils.isBlank(name)
                ? msgFromBundle("new.account") : name.trim();
        if(!newName.equals(name)) {
            internalAccount.setName(newName);
        }
        UInteger accountId = internalAccount.getId();
        log.info("Attempting update name to '" + newName + "', accountId=" + accountId);
        FacesContext fc = FacesContext.getCurrentInstance();
        localChangesCounter = 0;
        try {
            //noinspection ArraysAsListWithZeroOrOneArgument
            localChangesCounter = pointwiseUpdateQuery(eatlogDsl, ACCOUNT.ID, accountId,
                    Arrays.asList(
                            Pair.of(ACCOUNT.NAME, newName)
                    ));
        } catch (Throwable e) {
            log.warn("Failed update name '" + newName + "' for accountId=" + accountId, e);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.update.name"), ""));
            return;
        }
        fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("name.updated"),
                declensionValuedL10N(localChangesCounter, "change")));
        if(localChangesCounter > 0) {
            cacheService.evict(observedAccountByIdCache, accountId);
            updateSameAccountsInSessionsAndAddMsg(internalAccount,
                    internalAccount.getCalcedRoles(),
                    msgs,
                    applicationHolder.getSiteVisitorBySessionId());
            cacheService.evict(hoursCache, allocateSiteVisitorsKey); // for resort siteVisitorsOutputPanelId after name changed
        }
    }

    public void updateRoles() {
        TreeSet<String> rolesSet = convertRolesToNullIfEmpty(internalAccount);
        UInteger accountId = internalAccount.getId();
        log.info("Attempting update roles to " + rolesSet + ", accountId=" + accountId
                + " (" + internalAccount.getName() + ")");
        FacesContext fc = FacesContext.getCurrentInstance();
        localChangesCounter = 0;
        try {
            //noinspection ArraysAsListWithZeroOrOneArgument
            localChangesCounter = pointwiseUpdateQuery(eatlogDsl, ACCOUNT.ID, accountId,
                    Arrays.asList(
                            Pair.of(ACCOUNT.ROLES, rolesSet)
                    ));
        } catch (Throwable e) {
            log.warn("Failed update roles=" + rolesSet + " for accountId=" + accountId
                    + " (" + internalAccount.getName() + ")", e);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.update.roles"), ""));
            return;
        }
        fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("roles.updated"),
                declensionValuedL10N(localChangesCounter, "change")));
        if(localChangesCounter > 0) {
            updateSameAccountsInSessionsAndAddMsg(internalAccount,
                    internalAccount.getCalcedRoles(),
                    msgs);
        }
    }

    public void updateNullGradeEatlog() {
        internalAccount.setGradeEatlog(null);
        updateGradeEatlog();
    }

    public void updateGradeEatlog() {
        UInteger accountId = internalAccount.getId();
        log.info("Attempting update GradeEatlog to " + internalAccount.getGradeEatlog() + ", accountId=" + accountId
                + " (" + internalAccount.getName() + ")");
        FacesContext fc = FacesContext.getCurrentInstance();
        localChangesCounter = 0;
        try {
            //noinspection ArraysAsListWithZeroOrOneArgument
            localChangesCounter = pointwiseUpdateQuery(eatlogDsl, ACCOUNT.ID, accountId,
                    Arrays.asList(
                            Pair.of(ACCOUNT.GRADE_EATLOG, internalAccount.getGradeEatlog())
                    )
            );
        } catch (Throwable e) {
            log.warn("Failed update grade=" + internalAccount.getGradeEatlog() + " for accountId=" + accountId
                    + " (" + internalAccount.getName() + ")", e);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.update.grade"), ""));
            return;
        }
        fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("grade.updated"),
                declensionValuedL10N(localChangesCounter, "change")));
        if(localChangesCounter > 0) {
            updateSameAccountsInSessionsAndAddMsg(internalAccount,
                    internalAccount.getCalcedRoles(),
                    msgs);
        }
    }

    public void generateAuthStateValue() {
        authStateValue = ThreadLocalRandom.current().nextInt(999_999);
    }

    public String getAuthStateValue() {
        if(authStateValue == null)
            throw new IllegalStateException("authStateValue required");
        return "eatlog" + authStateValue;
    }

    public void clearAuthStateValue() {
        authStateValue = null;
    }

    public boolean isFetchAccepted() {
        return isAuthed() && !internalAccount.getIsBanned();
    }

    public boolean isAuthed() {
        return internalAccount != null;
    }

    public void attachAccount(InternalAccount internalAccount) {
        this.internalAccount = internalAccount;
        log.info("Successfully attached account=" + internalAccount);
        setLocale(internalAccount.getLocaleSegments());
        onLocaleChanged();
        String sessionId = extractSessionId();
        if(sessionId != null) {
            SiteVisitor siteVisitor = applicationHolder.getSiteVisitorBySessionId().get(sessionId);
            if(siteVisitor != null) {
                siteVisitor.setAccountId(internalAccount.getId());
                siteVisitor.setName(internalAccount.getName());
                siteVisitor.setBanned(internalAccount.getIsBanned());
                siteVisitor.setGooglePictureUrl(internalAccount.getGooglePictureUrl());
                if(geonameId != null) {
                    siteVisitor.setGeoInfo(cacheService.allocateGeoInfoByGeonameId(geonameId));
                }
                cacheService.evict(hoursCache, allocateSiteVisitorsKey, false); // for resort siteVisitorsOutputPanelId after id/name changed
            }
        }
    }

    public void detachAccount(boolean evictSiteVisitorsCache) {
        if(!isAuthed()) {
            return;
        }
        log.info("Successfully detached account=" + internalAccount);
        this.internalAccount = null;
        if(!evictSiteVisitorsCache) {
            return;
        }
        String sessionId = extractSessionId();
        if(sessionId == null) {
            return;
        }
        SiteVisitor siteVisitor = applicationHolder.getSiteVisitorBySessionId().get(sessionId);
        if(siteVisitor != null) {
            siteVisitor.clearFields();
            cacheService.evict(hoursCache, allocateSiteVisitorsKey, false); // for resort siteVisitorsOutputPanelId after id/name changed
        }
    }
}