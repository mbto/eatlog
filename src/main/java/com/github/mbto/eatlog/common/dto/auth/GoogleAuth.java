package com.github.mbto.eatlog.common.dto.auth;

import lombok.*;

@Getter
@Setter
@ToString
public class GoogleAuth {
    private String sub;
    private String name;
    private String picture;

    public GoogleAuth() {
    }

    public GoogleAuth(String sub) {
        this.sub = sub;
    }

    public GoogleAuth(String sub, String name, String picture) {
        this.sub = sub;
        this.name = name;
        this.picture = picture;
    }
}