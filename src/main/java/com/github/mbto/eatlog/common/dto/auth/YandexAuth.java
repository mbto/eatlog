package com.github.mbto.eatlog.common.dto.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class YandexAuth {
    private String yandexId;
    private String realName;
    private String avatarUrl;

    public YandexAuth() {
    }

    public YandexAuth(String yandexId) {
        this.yandexId = yandexId;
    }

    public YandexAuth(String yandexId, String realName, String avatarUrl) {
        this.yandexId = yandexId;
        this.realName = realName;
        this.avatarUrl = avatarUrl;
    }
}