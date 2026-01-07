package com.github.mbto.eatlog.webapp.session;

import com.github.jgonian.ipmath.Ipv4;
import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.dto.IpWrapper;
import com.github.mbto.eatlog.common.dto.SiteVisitor;
import com.github.mbto.eatlog.common.dto.auth.YandexAuth;
import com.github.mbto.eatlog.repository.AccountRepository;
import com.github.mbto.eatlog.service.ApplicationHolder;
import com.github.mbto.eatlog.service.CacheService;
import com.github.mbto.eatlog.service.DevLoginService;
import com.github.mbto.eatlog.service.MaxMindDbService;
import com.github.mbto.eatlog.utils.geoip.GeoInfo;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import static com.github.mbto.eatlog.common.Constants.msgs;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Account.ACCOUNT;
import static com.github.mbto.eatlog.service.CacheService.*;
import static com.github.mbto.eatlog.utils.ProjectUtils.*;
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
    private ApplicationHolder applicationHolder;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private MaxMindDbService maxMindDbService;
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private AccountRepository accountRepository;

    @Getter
    private Locale locale;
    @Getter
    private GeoInfo geoInfo;

    @Getter
    private InternalAccount internalAccount;

    @Getter
    @Setter
    private String authState;

    private int localChangesCounter;

    @PostConstruct
    public void init() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if(fc.isPostback()) {
            return;
        }
        if(log.isDebugEnabled()) {
            log.debug("\ninit");
        }
        if(!isAuthed()) {
            Locale newLocale = fc.getViewRoot().getLocale();
            if(newLocale == null) {
                newLocale = fc.getExternalContext().getRequestLocale();
            }
            setLocale(newLocale);
        } else {
            setLocale(internalAccount.getLocaleSegments());
        }
        onLocaleChanged();
        defineGeoInfoByIp();
        // for autologin while develop
        if(!applicationHolder.isDevEnvironment()) {
            return;
        }
        if(!fc.getExternalContext().getRequestParameterMap().containsKey("logout")) {
            YandexAuth yandexAuth = new YandexAuth("1493475743", "Body Thanks", "https://avatars.yandex.net/get-yapic/62162/FherlbVVwyc4GvMziINNiwIJ4U-1/islands-68");
            InternalAccount internalAccount
                    = accountRepository.findByYandexId(yandexAuth, "2.26.21.128");
            if(internalAccount != null) {
                internalAccount.calcRoles();
                attachAccount(internalAccount);
            }
        }
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
        String ip = extractIpFromFacesContext();
        if(isDebugEnabled) {
            log.debug("\ndefineGeoInfo ip=" + ip);
        }
        IpWrapper ipWrapper = new IpWrapper(UInteger.valueOf(Ipv4.of(ip).asBigInteger().longValue()));
        maxMindDbService.fillIpWrappersWithGeoInfos(List.of(ipWrapper), false);
        geoInfo = ipWrapper.getGeoInfo();
        if(isDebugEnabled) {
            log.debug("\ndefineGeoInfo ip=" + ip + ", geoInfo=" + geoInfo);
        }
    }

    @Getter
    @Setter
    private String newName;

    public void updateName() {
        newName = StringUtils.isBlank(newName)
                ? msgFromBundle("new.account")
                : newName.trim()
        ;
        FacesContext fc = FacesContext.getCurrentInstance();
        UInteger accountId = internalAccount.getId();
        log.info("Attempting update name to '" + newName + "', accountId=" + accountId);
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
            internalAccount.setName(newName);
            cacheService.evict(observedAccountByIdCache, accountId);
            updateSameAccountsInSessionsAndAddMsg(internalAccount,
                    internalAccount.getCalcedRoles(),
                    msgs,
                    applicationHolder.getSiteVisitorBySessionId());
            cacheService.evict(siteVisitorsCache, allocateSiteVisitorsKey);
            // for resort siteVisitorsOutputPanelId after name changed
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

    public boolean isFetchAccepted() {
        return isAuthed() && !internalAccount.getIsBanned();
    }

    public boolean isAuthed() {
        return internalAccount != null;
    }

    public void attachAccount(InternalAccount internalAccount) {
        this.internalAccount = internalAccount;
        this.newName = internalAccount.getName();
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
                siteVisitor.setSiteAvatarUrl(internalAccount.getSiteAvatarUrl());
                siteVisitor.setGeoInfo(geoInfo);
                cacheService.evict(siteVisitorsCache, allocateSiteVisitorsKey, false);
                // for resort siteVisitorsOutputPanelId after id/name changed
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
            cacheService.evict(siteVisitorsCache, allocateSiteVisitorsKey, false);
            // for resort siteVisitorsOutputPanelId after id/name changed
        }
    }

    @Autowired(required = false)
    private DevLoginService devLoginService;

    public void devLogin(String uniqueKey, String personaname, String authType) {
        if(!applicationHolder.isDevEnvironment()) {
            return;
        }
        if(authType.equals("yandex")) {
            YandexAuth yandexAuth = new YandexAuth(
                    uniqueKey,
                    personaname,
                    "https://avatars.yandex.net/get-yapic/62162/FherlbVVwyc4GvMziINNiwIJ4U-1/islands-68"
            );
            InternalAccount internalAccount
                    = devLoginService.findByYandexId(yandexAuth, "2.26.21.128");
            if(internalAccount != null) {
                internalAccount.calcRoles();
                attachAccount(internalAccount);
            } else {
                detachAccount(true);
            }
        }
    }
}