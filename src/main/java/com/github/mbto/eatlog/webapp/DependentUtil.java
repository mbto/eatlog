package com.github.mbto.eatlog.webapp;

import com.github.mbto.eatlog.utils.ProjectUtils;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    public static String humanLifetime(LocalDate date1,
                                       boolean includeEndDate) {
        return humanLifetime(date1.atStartOfDay(),
                LocalDate.now().atStartOfDay().plusDays(includeEndDate ? 1 : 0)
        );
    }

    public static String humanLifetime(LocalDate date1,
                                       LocalDate date2,
                                       boolean includeEndDate) {
        return humanLifetime(date1.atStartOfDay(),
                date2.atStartOfDay().plusDays(includeEndDate ? 1 : 0)
        );
    }

    public static String humanLifetime(LocalDateTime date1) {
        return humanLifetime(date1, LocalDateTime.now());
    }

    public static String humanLifetime(LocalDateTime date1,
                                       LocalDateTime date2) {
        if(date1 == null) {
            date1 = LocalDateTime.now();
        }
        if (date2.isBefore(date1)) {
            return null;
        }
        LocalDateTime tmp = date1;
        int y = (int) tmp.until(date2, ChronoUnit.YEARS);
        tmp = tmp.plusYears(y);
        int mn = (int) tmp.until(date2, ChronoUnit.MONTHS);
        tmp = tmp.plusMonths(mn);
        int d = (int) tmp.until(date2, ChronoUnit.DAYS);
        tmp = tmp.plusDays(d);
        int h = (int) (tmp.until(date2, ChronoUnit.HOURS) % 24);
        int m = (int) (tmp.until(date2, ChronoUnit.MINUTES) % 60);
        StringBuilder sb = new StringBuilder();
        boolean space = false;
        if (y > 0) {
            appendWithSpaceDecl(sb, space, y, "short.years", false);
            space = true;
        }
        if (mn > 0) {
            appendWithSpace(sb, space, mn, "short.months");
            space = true;
        }
        if (d > 0) {
            appendWithSpace(sb, space, d, "short.days");
            space = true;
        }
        if (h > 0) {
            appendWithSpace(sb, space, h, "short.hours");
            space = true;
        }
        if (m > 0) {
            appendWithSpace(sb, space, m, "short.mins");
            space = true;
        }
        return sb.isEmpty() ? msgFromBundle("short.now") : sb.toString();
    }

    public static String humanLifetime(long millis) {
        if (millis <= 0) {
            return msgFromBundle("short.now");
        }
        long totalSeconds = millis / 1_000;
        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime end = epoch.plusSeconds(totalSeconds);
        LocalDateTime tmp = epoch;
        int y = (int) tmp.until(end, ChronoUnit.YEARS);
        tmp = tmp.plusYears(y);
        int mn = (int) tmp.until(end, ChronoUnit.MONTHS);
        tmp = tmp.plusMonths(mn);
        int d = (int) tmp.until(end, ChronoUnit.DAYS);
        tmp = tmp.plusDays(d);
        int h = (int) (tmp.until(end, ChronoUnit.HOURS) % 24);
        int m = (int) (tmp.until(end, ChronoUnit.MINUTES) % 60);
        int s = (int) (tmp.until(end, ChronoUnit.SECONDS) % 60);
        StringBuilder sb = new StringBuilder();
        boolean space = false;
        if (y > 0) {
            appendWithSpaceDecl(sb, space, y, "time.min.years", false);
            space = true;
        }
        if (mn > 0) {
            appendWithSpace(sb, space, mn, "time.min.months");
            space = true;
        }
        if (d > 0) {
            appendWithSpace(sb, space, d, "time.min.days");
            space = true;
        }
        if (h > 0) {
            appendWithSpace(sb, space, h, "time.min.hours");
            space = true;
        }
        if (m > 0) {
            appendWithSpace(sb, space, m, "time.min.mins");
            space = true;
        }
        if (s > 0 || !space) {
            appendWithSpace(sb, space, s, "time.min.secs");
        }
        return sb.isEmpty() ? msgFromBundle("short.now") : sb.toString();
    }

    private static void appendWithSpace(StringBuilder sb,
                                        boolean space,
                                        long value,
                                        String bundleKey) {
        if (space) {
            sb.append(' ');
        }
        sb.append(value).append(msgFromBundle(bundleKey));
    }

    private static void appendWithSpaceDecl(StringBuilder sb,
                                            boolean space,
                                            long value,
                                            String keyPrefix,
                                            boolean addSeparate) {
        if (space) {
            sb.append(' ');
        }
        sb.append(declensionValuedL10N(value, keyPrefix, addSeparate));
    }

    public static String msgFromBundle(String key, Object... params) {
        return WebUtils.msgFromBundle(key, params);
    }

    public static String declensionValuedL10N(long value, String keyPrefix) {
        return ProjectUtils.declensionValuedL10N(value, keyPrefix, true);
    }

    public static String declensionValuedL10N(long value,
                                              String keyPrefix,
                                              boolean addSeparate) {
        return ProjectUtils.declensionValuedL10N(value, keyPrefix, addSeparate);
    }

    public static String declensionValued(long value,
                                          String leftPart,
                                          String oneValue,
                                          String fourValues,
                                          String fiveValues,
                                          boolean addSeparate) {
        boolean simpleDeclensionEnabled = Boolean.parseBoolean(msgFromBundle(BUNDLE_PROPS_SIMPLE_DECLENSION_ENABLED));
        return ProjectUtils.declensionValued(simpleDeclensionEnabled, value, leftPart, oneValue, fourValues, fiveValues, addSeparate);
    }

//    public static String declension(long value, String leftPart, String oneValue, String fourValues, String fiveValues) {
//        return ProjectUtils.declension(value, leftPart, oneValue, fourValues, fiveValues);
//    }
}