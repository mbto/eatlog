package com.github.mbto.eatlog.webapp.enums;

import lombok.Getter;

import java.util.stream.Stream;

public enum SettingEnum {
    OWNER_CONTACTS("owner_contacts"),
    RECOMMENDATIONS_JSON("recommendations_json"),
    DESCRIPTION_JSON("description_json"),
    DONATE_LINKS_JSON("donate_links_json"),
    METRIC_SCRIPTS("metric_scripts"),
    GOOGLE_CLIENT_ID("google_client_id"),
    GOOGLE_CLIENT_SECRET("google_client_secret"),
    GOOGLE_CALLBACK("google_callback"),
    GOOGLE_USERINFO_URL("google_userinfo_url"),
    ;

    public static final String[] OAUTH20_KEYS = Stream.of(
                    GOOGLE_CLIENT_ID,
                    GOOGLE_CLIENT_SECRET,
                    GOOGLE_CALLBACK,
                    GOOGLE_USERINFO_URL)
            .map(SettingEnum::getKey)
            .toArray(String[]::new);

    @Getter
    private final String key;

    SettingEnum(String key) {
        this.key = key;
    }
}