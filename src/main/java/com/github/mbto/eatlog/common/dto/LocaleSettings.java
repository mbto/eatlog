package com.github.mbto.eatlog.common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;
import java.util.Set;

@Getter
@EqualsAndHashCode
public class LocaleSettings {
    private final Integer order;
    private final String label;
    @Setter
    private Set<Locale> alternativeLocales;

    public LocaleSettings(Integer order, String label) {
        this.order = order;
        this.label = label;
    }

    @Override
    public String toString() {
        return "LocaleSettings{" +
                "order=" + order +
                ", label='" + label + '\'' +
                ", alternativeLocales.size=" + (alternativeLocales != null ? alternativeLocales.size() : "null") +
                '}';
    }
}