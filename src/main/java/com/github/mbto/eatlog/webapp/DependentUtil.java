package com.github.mbto.eatlog.webapp;

import com.github.mbto.eatlog.common.utils.ProjectUtils;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TreeSet;

import static com.github.mbto.eatlog.common.Constants.BUNDLE_PROPS_SIMPLE_DECLENSION_ENABLED;
import static com.github.mbto.eatlog.webapp.enums.RoleEnum.ALL_ROLES_NAMES;
import static com.github.mbto.eatlog.webapp.enums.RoleEnum.rolesContainerCreatorFunc;

/**
 * In spring boot with joinfaces this component with Singleton scope, not @Dependent scope
 */
@Dependent
@Named
@Slf4j
public class DependentUtil implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static TreeSet<String> mergeAllRoleNamesWithOthers(TreeSet<String> otherRoleNames) {
        TreeSet<String> roles = rolesContainerCreatorFunc.get();
        roles.addAll(ALL_ROLES_NAMES);
        if(!CollectionUtils.isEmpty(otherRoleNames)) {
            roles.addAll(otherRoleNames);
        }
        return roles;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean sendRedirectInternal(String redirectUrl) {
        return WebUtils.sendRedirectInternal(redirectUrl);
    }

//    public static String humanBoolean(Boolean value) {
//        return (value != null && value) ? "Yes" : "No";
//    }

    public static String humanLifetime(LocalDate date1, boolean includeEndDate) {
        return humanLifetime(date1.atStartOfDay(), LocalDate.now().atStartOfDay().plusDays(includeEndDate ? 1 : 0));
    }
    public static String humanLifetime(LocalDate date1, LocalDate date2, boolean includeEndDate) {
        return humanLifetime(date1.atStartOfDay(), date2.atStartOfDay().plusDays(includeEndDate ? 1 : 0));
    }
    public static String humanLifetime(LocalDateTime date1) {
        return humanLifetime(date1, LocalDateTime.now());
    }
    public static String humanLifetime(LocalDateTime date1, LocalDateTime date2) {
        Duration duration = Duration.between(date1, date2);
        long days = duration.toDaysPart();
        int hours = duration.toHoursPart();
        int mins = duration.toMinutesPart();
        if(days == 0 && hours == 0 && mins == 0)
            return msgFromBundle("short.now");
        StringBuilder sb = new StringBuilder();
        if(days > 0) sb.append(days).append(msgFromBundle("short.days"));
        if(hours > 0 || mins > 0) sb.append(" ");
        if(hours > 0) sb.append(hours).append(msgFromBundle("short.hours"));
        if(mins > 0) sb.append(" ");
        if(mins > 0) sb.append(mins).append(msgFromBundle("short.mins"));
        return sb.toString();
    }

    public static String msgFromBundle(String key, Object... params) {
        return WebUtils.msgFromBundle(key, params);
    }

    public static String declensionValuedL10N(long value, String keyPrefix) {
        return ProjectUtils.declensionValuedL10N(value, keyPrefix);
    }

    public static String declensionValued(long value, String leftPart,
                                          String oneValue, String fourValues, String fiveValues) {
        boolean simpleDeclensionEnabled = Boolean.parseBoolean(msgFromBundle(BUNDLE_PROPS_SIMPLE_DECLENSION_ENABLED));
        return ProjectUtils.declensionValued(simpleDeclensionEnabled, value, leftPart, oneValue, fourValues, fiveValues);
    }

//    public static String declension(long value, String leftPart, String oneValue, String fourValues, String fiveValues) {
//        return ProjectUtils.declension(value, leftPart, oneValue, fourValues, fiveValues);
//    }
}