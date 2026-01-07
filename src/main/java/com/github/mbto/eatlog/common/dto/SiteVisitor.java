package com.github.mbto.eatlog.common.dto;

import com.github.mbto.eatlog.utils.geoip.GeoInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jooq.types.UInteger;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SiteVisitor {
    @EqualsAndHashCode.Include
    private UInteger accountId;
    private String name;
    private boolean isBanned;
    private String siteAvatarUrl;
    /**
     * Using GeoInfo, not geonameId
     * The object is convenient to use in .xhtml templates
     */
    private GeoInfo geoInfo;
    private LocalDateTime expiredDateTime;

    public SiteVisitor() {
        updateExpiredDateTime();
    }

    public void clearFields() {
        this.accountId = null;
        this.name = null;
        this.isBanned = false;
        this.siteAvatarUrl = null;
        this.geoInfo = null;
        this.expiredDateTime = null;
    }

    public void updateExpiredDateTime() {
        expiredDateTime = LocalDateTime.now().plusHours(1);
    }
}