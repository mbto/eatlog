package com.github.mbto.eatlog.common.mapper;

import com.github.mbto.eatlog.common.dto.GeoInfo;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.types.UInteger;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GeoInfoMapper implements RecordMapper<Record, GeoInfo> {
    private final Set<Locale> availableLocales;

    public GeoInfoMapper(Set<Locale> availableLocales) {
        this.availableLocales = availableLocales;
    }

    @Override
    public GeoInfo map(Record r) {
        GeoInfo geoInfo = new GeoInfo(availableLocales);
        geoInfo.setGeonameId(r.get("geoname_id", UInteger.class));
        geoInfo.setEmoji(r.get("emoji", String.class));
        for (Map.Entry<Locale, GeoInfo.Location> entry : geoInfo.getLocationByLocale()
                .entrySet()) {
            Locale locale = entry.getKey();
            GeoInfo.Location location = entry.getValue();
            String language = locale.getLanguage();
            location.setCountry(r.get("country_" + language, String.class));
            location.setCity(r.get("city_" + language, String.class));
            location.setSubdivision1(r.get("subDiv1_" + language, String.class));
            location.setSubdivision2(r.get("subDiv2_" + language, String.class));
        }
        return geoInfo;
    }
}
