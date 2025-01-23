package com.github.mbto.eatlog.webapp.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Named;

import java.time.LocalDateTime;

import static com.github.mbto.eatlog.common.Constants.*;

@FacesConverter(value = "dateTimeToDateHHmmssConverter")
@Named
public class DateTimeToDateHHmmssConverter implements Converter<LocalDateTime> {
    @Override
    public LocalDateTime getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null)
            return null;
        return LocalDateTime.parse(value, DDMMYYYY_HHMMSS_PATTERN);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, LocalDateTime value) {
        return value == null ? "" : DDMMYYYY_HHMMSS_PATTERN.format(value);
    }
}