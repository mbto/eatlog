package com.github.mbto.eatlog.webapp.error;

//import org.springframework.boot.web.error.ErrorAttributeOptions;
//import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.WebRequest;
//
//import java.util.Map;
//
//@Profile("default")
//@Component
//public class ErrorAttributes extends DefaultErrorAttributes {
//    @Override
//    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions errorAttributeOptions) {
//        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, errorAttributeOptions);
//        errorAttributes.remove("path");
//        errorAttributes.remove("trace");
//        errorAttributes.remove("message");
//
//        return errorAttributes;
//    }
//}