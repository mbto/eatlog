package com.github.mbto.eatlog.common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jooq.types.UInteger;

import java.util.*;
import java.util.function.Function;

import static com.github.mbto.eatlog.common.dto.GeoInfo.Location.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class GeoInfo {
    @Getter
    @Setter
    @EqualsAndHashCode.Include
    private UInteger geonameId;
    @Getter
    @Setter
    private String emoji;
    @Getter
    private final Map<Locale, Location> locationByLocale = new LinkedHashMap<>();

    public GeoInfo(Set<Locale> availableLocales) {
        for (Locale availableLocale : availableLocales) {
            locationByLocale.put(availableLocale, new Location());
        }
    }

    /**
     * We can change to return List<String>, and use String.join with custom separators
     * If useCache==true, returned String will be cached only with one separator
     */
    public String buildLocationRaw(Locale locale, String separator, boolean useCache) {
        Location location = locationByLocale.get(locale);
        if(location == null) {
            location = new Location();
            locationByLocale.put(locale, location);
        } else if(useCache && location.getLocationRaw() != null) {
            return location.getLocationRaw();
        }
        String subdivision2 = buildOneOfExistedValue(locale, location, getSubdivision2Func);
        String subdivision1 = buildOneOfExistedValue(locale, location, getSubdivision1Func);
        String city = buildOneOfExistedValue(locale, location, getCityFunc);
        String country = buildOneOfExistedValue(locale, location, getCountryFunc);
        final boolean[] clearFlags = new boolean[3];
        if(subdivision2 != null) {
            if (StringUtils.equals(country, subdivision2)
             || StringUtils.equals(city, subdivision2)
             || StringUtils.equals(subdivision1, subdivision2)) {
                clearFlags[0] = true; // clear sub2
            }
        }
        if(subdivision1 != null) {
            if (StringUtils.equals(country, subdivision1)
             || StringUtils.equals(city, subdivision1)) {
                clearFlags[1] = true; // clear sub1
            }
        }
        if(city != null) {
            if (StringUtils.equals(country, city)) {
                clearFlags[2] = true; // clear city
            }
        }
        if(clearFlags[0]) subdivision2 = null;
        if(clearFlags[1]) subdivision1 = null;
        if(clearFlags[2]) city = null;
        StringBuilder sb = new StringBuilder();
        if (emoji != null) sb.append(emoji);
        if (country != null || city != null || subdivision1 != null || subdivision2 != null) sb.append(" ");
        if (country != null) sb.append(country);
        if (city != null || subdivision1 != null || subdivision2 != null) sb.append(separator);
        if (city != null) sb.append(city);
        if (subdivision1 != null || subdivision2 != null) sb.append(separator);
        if (subdivision1 != null) sb.append(subdivision1);
        if (subdivision2 != null) sb.append(separator);
        if (subdivision2 != null) sb.append(subdivision2);
        String locationRaw = sb.toString();
        if(useCache) {
            location.setLocationRaw(locationRaw);
        }
        return locationRaw;
    }

    private String buildOneOfExistedValue(Locale locale,
                                          Location location,
                                          Function<Location, String> valueByLocationFunc) {
        String value = valueByLocationFunc.apply(location);
        if(value != null) {
            return value;
        }
        for (Map.Entry<Locale, Location> entry : locationByLocale.entrySet()) {
            Locale otherLocale = entry.getKey();
            if(!locale.equals(otherLocale)) {
                Location otherLocation = entry.getValue();
                String otherValue = valueByLocationFunc.apply(otherLocation);
                if(otherValue != null)
                    return otherValue;
            }
        }
        return null;
    }

    @Getter
    @Setter
    @ToString
    public static class Location {
        private String country;
        private String city;
        private String subdivision1;
        private String subdivision2;

        private String locationRaw;

        public static final Function<Location, String> getCountryFunc = Location::getCountry;
        public static final Function<Location, String> getCityFunc = Location::getCity;
        public static final Function<Location, String> getSubdivision1Func = Location::getSubdivision1;
        public static final Function<Location, String> getSubdivision2Func = Location::getSubdivision2;
    }
}