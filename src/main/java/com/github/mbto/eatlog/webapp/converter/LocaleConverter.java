package com.github.mbto.eatlog.webapp.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import org.apache.commons.lang3.LocaleUtils;

import java.util.Locale;

@FacesConverter(forClass = Locale.class)
public class LocaleConverter implements Converter<Locale> {
    @Override
    public Locale getAsObject(FacesContext context, UIComponent component, String value) {
        return LocaleUtils.toLocale(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Locale value) {
        return value.toString();
    }
}