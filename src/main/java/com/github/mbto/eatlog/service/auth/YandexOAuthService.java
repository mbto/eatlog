package com.github.mbto.eatlog.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mbto.eatlog.common.dto.auth.YandexAuth;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Setting;
import com.github.mbto.eatlog.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static com.github.mbto.eatlog.common.Constants.ACCOUNT_PAGE;
import static com.github.mbto.eatlog.utils.ProjectUtils.urlEncode;
import static com.github.mbto.eatlog.webapp.WebUtils.getBaseUrl;
import static com.github.mbto.eatlog.webapp.enums.SettingEnum.YANDEX_CLIENT_ID;
import static com.github.mbto.eatlog.webapp.enums.SettingEnum.YANDEX_CLIENT_SECRET;

@Service
@Slf4j
public class YandexOAuthService {
    private static final String AUTH_URL = "https://oauth.yandex.ru/authorize";
    private static final String TOKEN_URL = "https://oauth.yandex.ru/token";
    private static final String API_URL = "https://login.yandex.ru/info";

    @Autowired
    private CacheService cacheService;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpClient httpClient;

    public String buildLoginUrl(String state) {
        String baseUrl = getBaseUrl();
        String returnUrl = baseUrl + ACCOUNT_PAGE;
        Setting setting = (Setting) cacheService.allocateSettingByKey(YANDEX_CLIENT_ID.getKey(),
                false, true, null);
        Map<String, String> p = Map.of(
                "response_type", "code",
                "client_id", setting.getValue(),
                "scope", "login:info login:avatar",
                "state", state,
                "redirect_uri", returnUrl
        );
        return AUTH_URL + "?" + urlEncode(p);
    }

    public YandexAuth fetchUserProfile(String code, String redirectUri) throws Exception {
        String token = exchangeCode(code, redirectUri);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "OAuth " + token)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        int statusCode = resp.statusCode();
        if(statusCode != 200) {
            throw new IllegalArgumentException("Profile fetch failed:"
                                               + " statusCode=" + statusCode
                                               + ", body=" + resp.body());
        }
        JsonNode tree = objectMapper.readTree(resp.body());
        String id = tree.get("id").asText();
        String realName = tree.get("real_name").asText(null);
        boolean isAvatarEmpty = tree.get("is_avatar_empty").asBoolean(false);
        String avatarUrl = null;
        if (!isAvatarEmpty) {
            String avatarId = tree.get("default_avatar_id").asText(null);
            if(!StringUtils.isBlank(avatarId)) {
                avatarUrl = "https://avatars.yandex.net/get-yapic/" + avatarId + "/islands-68";
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("id=" + id
                      + ", realName=" + realName
                      + ", isAvatarEmpty=" + isAvatarEmpty
                      + ", avatarUrl=" + avatarUrl
            );
        }
        return new YandexAuth(id, realName, avatarUrl);
    }

    public String exchangeCode(String code, String redirectUri) throws Exception {
        Setting yandexClientIdSetting = (Setting) cacheService.allocateSettingByKey(
                YANDEX_CLIENT_ID.getKey(), false, true, null);
        Setting yandexClientSecretSetting = (Setting) cacheService.allocateSettingByKey(
                YANDEX_CLIENT_SECRET.getKey(), false, true, null);

        Map<String, String> p = Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "client_id", yandexClientIdSetting.getValue(),
                "client_secret", yandexClientSecretSetting.getValue(),
                "redirect_uri", redirectUri
        );
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(urlEncode(p)))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        int statusCode = resp.statusCode();
        if (statusCode != 200) {
            throw new IllegalArgumentException("Token exchange failed:"
                                               + " statusCode=" + statusCode
                                               + ", body=" + resp.body());
        }
        return objectMapper.readTree(resp.body()).get("access_token").asText();
    }
}