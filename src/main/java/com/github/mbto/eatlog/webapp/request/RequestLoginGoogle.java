package com.github.mbto.eatlog.webapp.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.dto.auth.GoogleAuth;
import com.github.mbto.eatlog.service.AccountService;
import com.github.mbto.eatlog.service.CacheService;
import com.github.mbto.eatlog.webapp.DependentUtil;
import com.github.mbto.eatlog.webapp.session.SessionAccount;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
import com.github.scribejava.core.oauth.OAuth20Service;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.FacesException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Setting.SETTING;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.addDotsForBigString;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;
import static com.github.mbto.eatlog.webapp.enums.SettingEnum.*;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;

/*
https://console.cloud.google.com/apis/credentials?hl=ru&organizationId=0&project=edalog
https://developers.google.com/identity/openid-connect/openid-connect?hl=ru#an-id-tokens-payload
https://developers.google.com/identity/protocols/oauth2/web-server?hl=ru#python
https://github.com/scribejava/scribejava/blob/master/scribejava-apis/src/test/java/com/github/scribejava/apis/examples/Google20Example.java
*/
@RequestScoped
@Named
@Slf4j
public class RequestLoginGoogle {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private AccountService accountService;
    @Autowired
    private SessionAccount sessionAccount;
    @Autowired
    private DependentUtil util;

/* Accept OAuth20 params
https://localhost:8080/account?logout
https://localhost:8080/account?state=eatlog425542&code=4%2F0AVHEtk4An-5HJSQlhrB1NxDS1lo7rVuCAGSPxOP3l46rMSG1W8tYo5oygQG8ORdLsqiCsg&scope=profile+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile
*/
    public void onInvokeApplication() {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled)
            log.debug("\nonInvokeApplication");
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> requestParamValueByKey = fc.getExternalContext().getRequestParameterMap();
        if(requestParamValueByKey.containsKey(LOGOUT_REQUEST_PARAM)) {
            if(sessionAccount.isAuthed()) {
                fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                        msgFromBundle("you.logged.out"), ""));
                detachAccountAndInvalidateSession();
            }
            return;
        }
        String state = requestParamValueByKey.get(STATE_REQUEST_PARAM);
        String code = requestParamValueByKey.get(CODE_REQUEST_PARAM);
        if(StringUtils.isAnyBlank(state, code)) {
            return;
        }
        Map<String, String> settingValueByKey = fetchOAuth20SettingValueByKey();
        if(!validateSettingValues(settingValueByKey)) {
            log.warn("Failed auth, due failed validation setting values, settingValueByKey=" + settingValueByKey);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("auth.unavailable"),
                    msgFromBundle("failed.validation.setting.values")));
            return;
        }
        log.info("Try auth, requestParamValueByKey=" + requestParamValueByKey);
        GoogleAuth auth;
        try {
            if(!state.equals(sessionAccount.getAuthStateValue())) {
                // throw for invalidate session
                throw new FacesException(msgFromBundle("invalid.value.of.param",
                        addDotsForBigString(state, 12),
                        STATE_REQUEST_PARAM));
            }
            try(OAuth20Service service = buildOAuth20Service(settingValueByKey)) {
                AccessTokenRequestParams accessTokenRequestParams = AccessTokenRequestParams.create(code);
                OAuth2AccessToken accessToken = service.getAccessToken(accessTokenRequestParams);
                if (StringUtils.isBlank(accessToken.getAccessToken())/* || accessToken.getExpiresIn() != null*/) {
                    // throw for invalidate session
                    throw new FacesException(msgFromBundle("invalid.access.token"));
                }
                String userinfoUrl = GOOGLE_USERINFO_URL.getKey();
                OAuthRequest request = new OAuthRequest(Verb.GET, settingValueByKey.get(userinfoUrl));
                service.signRequest(accessToken, request);
                try (Response response = service.execute(request)) {
                    JsonNode jsonNode = objectMapper.readTree(response.getBody());
                    if(isDebugEnabled)
                        log.debug("\njsonNode.toPrettyString()=" + jsonNode.toPrettyString());
                    String sub = Optional.ofNullable(jsonNode.get("sub"))
                            .map(JsonNode::textValue)
                            .orElseThrow(() -> new FacesException(msgFromBundle("field.sub.required", userinfoUrl)));
                    String name = Optional.ofNullable(jsonNode.get("name"))
                            .map(JsonNode::textValue)
                            .filter(StringUtils::isNotBlank)
                            .orElse(null);
                    String picture = Optional.ofNullable(jsonNode.get("picture"))
                            .map(JsonNode::textValue)
                            .filter(StringUtils::isNotBlank)
                            .orElse(null);
                    auth = new GoogleAuth();
                    auth.setSub(sub);
                    auth.setName(name);
                    auth.setPicture(picture);
                }
            }
        } catch (Throwable e) {
            log.warn("Failed auth, requestParamValueByKey=" + requestParamValueByKey, e);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.auth"), e.toString()));
            detachAccountAndInvalidateSession();
            return;
        } finally {
            sessionAccount.clearAuthStateValue();
        }
        InternalAccount internalAccount = accountService.findOrCreateAccount(
                auth,
                sessionAccount.getLocale().toString(),
                sessionAccount.getGeonameId());
        sessionAccount.attachAccount(internalAccount);
        util.sendRedirectInternal("account?id=" + internalAccount.getId());
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

    public void redirectToOAuth20Provider() throws Throwable {
        if (log.isDebugEnabled())
            log.debug("\nredirectToOAuth20Provider");
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> settingValueByKey = fetchOAuth20SettingValueByKey();
        if(!validateSettingValues(settingValueByKey)) {
            log.warn("Failed build authorizationUrl, due failed validation setting values, settingValueByKey=" + settingValueByKey);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("auth.unavailable"),
                    msgFromBundle("failed.validation.setting.values")));
            return;
        }
        String authorizationUrl;
        try (OAuth20Service service = buildOAuth20Service(settingValueByKey)) {
            Map<String, String> additionalValueByKey = new HashMap<>();
            additionalValueByKey.put("access_type", "offline");
//          additionalValueByKey.put("prompt", "consent");
            sessionAccount.generateAuthStateValue();
            authorizationUrl = service.createAuthorizationUrlBuilder()
                    .state(sessionAccount.getAuthStateValue())
                    .additionalParams(additionalValueByKey)
                    .build();
        } catch (Throwable e) {
            log.warn("Failed build authorizationUrl", e);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.build.authorizationUrl"),
                    e.toString()));
            return;
        }
        log.info("Builded redirect authorizationUrl=" + authorizationUrl);
        ExternalContext ec = fc.getExternalContext();
        ec.redirect(authorizationUrl);
        fc.responseComplete();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean validateSettingValues(Map<String, String> settingValueByKey) {
        return Stream.of(OAUTH20_KEYS)
                .map(settingValueByKey::get)
                .noneMatch(StringUtils::isBlank);
    }

    private Map<String, String> fetchOAuth20SettingValueByKey() {
        return eatlogDsl.selectFrom(SETTING)
                .where(SETTING.KEY.in(OAUTH20_KEYS))
                .fetchMap(SETTING.KEY, SETTING.VALUE);
    }

    private OAuth20Service buildOAuth20Service(Map<String, String> settingValueByKey) {
        return new ServiceBuilder(settingValueByKey.get(GOOGLE_CLIENT_ID.getKey()))
                .apiSecret(settingValueByKey.get(GOOGLE_CLIENT_SECRET.getKey()))
                .defaultScope("profile")
                .callback(settingValueByKey.get(GOOGLE_CALLBACK.getKey()))
                .build(com.github.scribejava.apis.GoogleApi20.instance());
    }
}