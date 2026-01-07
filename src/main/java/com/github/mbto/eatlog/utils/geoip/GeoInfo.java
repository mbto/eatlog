package com.github.mbto.eatlog.utils.geoip;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Locale;
import java.util.Objects;

import static com.github.mbto.eatlog.common.Constants.RU_LOCALE_LANGUAGE;

@Getter
@Setter
@ToString
public final class GeoInfo {
    private final Long geonameId;
    private final String countryEmoji;
    private final String locationRu;
    private final String locationEn;
    private final String locationMix;

    public GeoInfo(
            Long geonameId,
            String countryEmoji,
            String locationRu,
            String locationEn,
            String locationMix
    ) {
        this.geonameId = geonameId;
        this.countryEmoji = countryEmoji;
        this.locationRu = locationRu;
        this.locationEn = locationEn;
        this.locationMix = locationMix;
    }

    public String locationByLocaleLanguage(Locale locale) {
        if(RU_LOCALE_LANGUAGE.equals(locale.getLanguage())) {
            return locationMix;
        }
        return locationEn;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeoInfo geoInfo)) return false;
        return Objects.equals(locationMix, geoInfo.locationMix);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(locationMix);
    }
}