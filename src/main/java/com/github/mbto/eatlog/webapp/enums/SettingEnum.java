package com.github.mbto.eatlog.webapp.enums;

import lombok.Getter;

/*
owner_contacts
recommendations_json
description_json
donate_links_json
metric_scripts
yandex_client_id
yandex_client_secret
*/
public enum SettingEnum {
    OWNER_CONTACTS("owner_contacts"),
    RECOMMENDATIONS_JSON("recommendations_json"),
    DESCRIPTION_JSON("description_json"),
    DONATE_LINKS_JSON("donate_links_json"),
    METRIC_SCRIPTS("metric_scripts"),
    YANDEX_CLIENT_ID("yandex_client_id"),
    YANDEX_CLIENT_SECRET("yandex_client_secret"),
    ;

    @Getter
    private final String key;

    SettingEnum(String key) {
        this.key = key;
    }
}