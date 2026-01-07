package com.github.mbto.eatlog.webapp.validator;

import org.apache.commons.lang3.StringUtils;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

@FacesValidator(value = "textEditorNotBlankValidator")
public class TextEditorNotBlankValidator implements Validator<String> {
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        if(value == null) {
            throwValidatorException(value);
        }
        String originalValue = value;
        if(value.length() >= 7) {
            value = value.substring(3, value.length() - 4); // <p>value</p>
        }
        if(StringUtils.isBlank(value)) {
            throwValidatorException(originalValue);
        }
    }

    private void throwValidatorException(String value) {
        throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_WARN,
                "Invalid value '" + value + "'", ""));
    }
}