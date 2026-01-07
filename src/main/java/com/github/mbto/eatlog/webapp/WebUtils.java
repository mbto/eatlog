package com.github.mbto.eatlog.webapp;

import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.dto.CalcedRoles;
import com.github.mbto.eatlog.common.dto.SiteVisitor;
import com.github.mbto.eatlog.utils.ProjectUtils;
import com.github.mbto.eatlog.webapp.session.SessionAccount;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StandardSessionFacade;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.extensions.component.legend.Legend;
import org.springframework.util.CollectionUtils;
import software.xdev.chartjs.model.charts.Chart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.enums.ScalesPosition;
import software.xdev.chartjs.model.options.Font;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.options.elements.Fill;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.CartesianScaleOptions;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.mbto.eatlog.common.Constants.DDMMYYYY_PATTERN;
import static com.github.mbto.eatlog.common.Constants.msgs;
import static com.github.mbto.eatlog.utils.ProjectUtils.*;
import static com.github.mbto.eatlog.webapp.DependentUtil.humanLifetime;
import static com.github.mbto.eatlog.webapp.enums.RoleEnum.rolesContainerCreatorFunc;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;

@Slf4j
public class WebUtils {
    public static String msgFromBundle(String key, Object... params) {
        FacesContext fc = FacesContext.getCurrentInstance();
        String placeholder = fc.getApplication()
                .getResourceBundle(fc, "l10n")
                .getString(key);
        if(params == null || params.length == 0)
            return placeholder;
        return String.format(placeholder, params);
    }

    public static ValidatorException makeValidatorException(String value, String details) {
        return new ValidatorException(new FacesMessage(SEVERITY_WARN,
                msgFromBundle("failed.validation.value", value),
                details));
    }

    public static String getBaseUrl() {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest req = (HttpServletRequest) ec.getRequest();
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String ctx = req.getContextPath();
        StringBuilder sb = new StringBuilder(scheme).append("://").append(host);
        if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
            sb.append(':').append(port);
        }
        sb.append(ctx);
        return sb.toString();
    }

    public static String extractIpFromFacesContext() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        String ip = ec.getRequestHeaderMap().get("X-Real-IP"); // nginx
        if(StringUtils.isBlank(ip)) {
            HttpServletRequest req = (HttpServletRequest) ec.getRequest();
            ip = req.getRemoteAddr();
        }
//        return "2.26.21.128"; // todo: for tests
        return ip;
    }

    public static boolean sendRedirectInternal(String redirectUrl) {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        redirectUrl = ec.getRequestContextPath() + "/" + redirectUrl;
        if (log.isDebugEnabled()) {
            log.debug("\nsendRedirectInternal result redirectUrl=" + redirectUrl);
        }
        try {
            ec.redirect(redirectUrl);
            return true;
        } catch (Throwable e) {
            String msg = "Failed redirect to redirectUrl=" + redirectUrl;
            log.warn(msg, e);
//            fc.addMessage(msgs, new FacesMessage(FacesMessage.SEVERITY_WARN, msg, ""));
            return false;
        } finally {
            fc.responseComplete();
        }
    }

    public static boolean sendRedirect(String redirectUrl) {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        if (log.isDebugEnabled()) {
            log.debug("\nsendRedirect result redirectUrl=" + redirectUrl);
        }
        try {
            ec.redirect(redirectUrl);
            return true;
        } catch (Throwable e) {
            String msg = "Failed redirect to redirectUrl=" + redirectUrl;
            log.warn(msg, e);
//            fc.addMessage(msgs, new FacesMessage(FacesMessage.SEVERITY_WARN, msg, ""));
            return false;
        } finally {
            fc.responseComplete();
        }
    }
    /*
    public static LineChartModel generateLineChartModelWithLocalDateLabels(
            Map<LocalDate, ? extends Number> valueNumberByDateLabel,
            String legend, String titleText) {
        if(valueNumberByDateLabel == null || valueNumberByDateLabel.size() < 2) {
            return null;
        }
        Map<String, Number> valueNumberByLabel = valueNumberByDateLabel.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().format(DDMMYYYY_PATTERN),
                        Map.Entry::getValue,
                        (num1, num2) -> {
                            throw new IllegalStateException("Dublicate keys founded with num1=" + num1 + ", num2=" + num2);
                        }, LinkedHashMap::new));
        return generateLineChartModelWithStringLabels(valueNumberByLabel, legend, titleText);
    }

    public static LineChartModel generateLineChartModelWithStringLabels(
            Map<String, ? extends Number> valueNumberByLabel,
            String legend, String titleText) {
        if(valueNumberByLabel == null || valueNumberByLabel.size() < 2) {
            return null;
        }
        LineChartDataSet dataSet = new LineChartDataSet();
        dataSet.setLabel(msgFromBundle("weight.named", legend));
        dataSet.setYaxisID("left-y-axis");
        dataSet.setFill(true);
        dataSet.setBackgroundColor("rgb(255, 200, 200, 0.2)");
        dataSet.setPointBackgroundColor("rgb(0, 162, 0)");
        dataSet.setTension(0.5);
        Collection<? extends Number> values = valueNumberByLabel.values();
        dataSet.setData(new ArrayList<>(values));

        ChartData chartData = new ChartData();
        chartData.addChartDataSet(dataSet);
        chartData.setLabels(new ArrayList<>(valueNumberByLabel.keySet()));

        Number min = values.stream()
                .min(numberComparator)
                .orElse(null);
        Number max = values.stream()
                .max(numberComparator)
                .orElse(null);
        CartesianLinearAxes linearAxesLeft = new CartesianLinearAxes();
        linearAxesLeft.setId("left-y-axis");
        linearAxesLeft.setPosition("left");
        linearAxesLeft.setMin(min);
        linearAxesLeft.setMax(max);
        CartesianLinearAxes linearAxesRight = new CartesianLinearAxes();
        linearAxesRight.setId("right-y-axis");
        linearAxesRight.setPosition("right");
        linearAxesRight.setMin(min);
        linearAxesRight.setMax(max);

        CartesianScales cScales = new CartesianScales();
        //noinspection SuspiciousNameCombination
        cScales.addYAxesData(linearAxesLeft);
        //noinspection SuspiciousNameCombination
        cScales.addYAxesData(linearAxesRight);

        Title title = null;
        if(titleText != null) {
            title = new Title();
            title.setDisplay(true);
            title.setText(titleText);
            title.setFontSize(15);
            title.setFontColor("rgb(0, 0, 0)");
        }

        LineChartOptions options = new LineChartOptions();
        options.setScales(cScales);
        if(title != null) {
            options.setTitle(title);
        }
//        options.setSpanGaps(true);

        LineChartModel cartesianLinerModel = new LineChartModel();
        cartesianLinerModel.setData(chartData);
        cartesianLinerModel.setOptions(options);
        return cartesianLinerModel;
    }
    */

    public static String generateLineChartModelWithLocalDateLabels(
            Map<LocalDate, ? extends Number> valueNumberByDateLabel,
            String legend, String titleText) {

        if (valueNumberByDateLabel == null || valueNumberByDateLabel.size() < 2) {
            return null;
        }
        Map<String, Number> valueNumberByLabel = valueNumberByDateLabel.entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> e.getKey().format(DDMMYYYY_PATTERN),
                Map.Entry::getValue,
                (num1, num2) -> {
                    throw new IllegalStateException("Dublicate keys founded with num1=" + num1 + ", num2=" + num2);
                },
                LinkedHashMap::new));
        return generateLineChartModelWithStringLabels(valueNumberByLabel, legend, titleText);
    }

    public static String generateLineChartModelWithStringLabels(
            Map<String, ? extends Number> valueNumberByLabel,
            String legend, String titleText) {
        if (valueNumberByLabel == null || valueNumberByLabel.size() < 2) {
            return null;
        }
        Collection<? extends Number> values = valueNumberByLabel.values();
        LineDataset dataset = new LineDataset()
                .setLabel(msgFromBundle("weight.named", legend))
                .setYAxisID("left-y-axis")
                .setFill(Boolean.TRUE)
                .setBackgroundColor(new RGBAColor(255, 200, 200, 0.2))
                .setPointBackgroundColor(List.of(new RGBAColor(0, 162, 0)))
                .setTension(0.5f)
                .setData(new ArrayList<>(values))
                ;
        LineData lineData = new LineData()
                .setLabels(new ArrayList<>(valueNumberByLabel.keySet()))
                .addDataset(dataset)
                ;
        Number min = values.stream().min(numberComparator).orElse(null);
        Number max = values.stream().max(numberComparator).orElse(null);
        CartesianScaleOptions leftScale = new CartesianScaleOptions()
//                .setId("left-y-axis")
                .setPosition(ScalesPosition.LEFT)
                .setMin(min)
                .setMax(max);

        CartesianScaleOptions rightScale = new CartesianScaleOptions()
//                .setId("right-y-axis")
                .setPosition(ScalesPosition.RIGHT)
                .setMin(min)
                .setMax(max);

//        Scales scales = new Scales().addYAxes(leftScale, rightScale);
        Scales scales = new Scales();
        scales
                .addScale(Scales.ScaleAxis.Y, leftScale)
                .addScale(Scales.ScaleAxis.Y, rightScale)
        ;

        /* 4. title */
        Title title = null;
        if(titleText != null) {
            title = new Title().setDisplay(true)
                    .setText(titleText)
                    .setFont(new Font()
                            .setSize(15)
                    ).setColor(new RGBAColor(0, 0, 0))
            ;
        }
        LineOptions options = new LineOptions()
                .setScales(scales)
                .setPlugins(new Plugins()
                        .setTitle(title)
                )
//                .setSpanGaps(true)
                .setResponsive(true)
                .setMaintainAspectRatio(false)
                ;
        return new LineChart(lineData, options).toJson();
    }

    public static Object extractStandardSessionFacadeObject() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        return ec.getSession(false);
    }

    public static String extractSessionId() {
        Object standardSessionFacadeObject = extractStandardSessionFacadeObject();
        if(standardSessionFacadeObject instanceof HttpSession httpSession) {
            return httpSession.getId();
        }
        return null;
    }

    public static Manager extractSessionsManager() throws Throwable {
        Object standardSessionFacadeObject = extractStandardSessionFacadeObject();
        if(!(standardSessionFacadeObject instanceof StandardSessionFacade))
            return null;
        Field sessionField = standardSessionFacadeObject.getClass()
                .getDeclaredField("session");
        sessionField.setAccessible(true);
        Object standardSessionObject = sessionField.get(standardSessionFacadeObject);
        if(!(standardSessionObject instanceof StandardSession standardSession))
            return null;
        return standardSession.getManager();
    }

    public static void updateSameAccountsInSessionsAndAddMsg(InternalAccount sourceInternalAccount,
                                                             CalcedRoles calcedRolesOfInitiator,
                                                             String failedMsgClientId) {
        updateSameAccountsInSessionsAndAddMsg(sourceInternalAccount,
                calcedRolesOfInitiator,
                failedMsgClientId,
                null);
    }
    public static void updateSameAccountsInSessionsAndAddMsg(InternalAccount sourceInternalAccount,
                                                             CalcedRoles calcedRolesOfInitiator,
                                                             String failedMsgClientId,
                                                             Map<String, SiteVisitor> siteVisitorBySessionId) {
        boolean isInitiatorOwnerOrAdmin = calcedRolesOfInitiator.isOwnerOrAdmin();
        FacesContext fc = FacesContext.getCurrentInstance();
        int sessionsUpdatedCount;
        try {
            sessionsUpdatedCount = updateSameAccountsInSessions(sourceInternalAccount, siteVisitorBySessionId);
        } catch (Throwable e) {
            log.warn("Failed update sessions from sourceInternalAccount=" + sourceInternalAccount, e);
            if(isInitiatorOwnerOrAdmin) {
                fc.addMessage(failedMsgClientId, new FacesMessage(SEVERITY_WARN,
                        msgFromBundle("failed.update.sessions"), ""));
            }
            return;
        }
        if(isInitiatorOwnerOrAdmin) {
            fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("sessions.updated"),
                    declensionValuedL10N(sessionsUpdatedCount, "change")));
        }
    }

    public static int updateSameAccountsInSessions(InternalAccount sourceInternalAccount,
                                                   Map<String, SiteVisitor> siteVisitorBySessionId) throws Throwable {
        Manager manager = extractSessionsManager();
        if(manager == null)
            return 0;
        int updatedCount = 0;
//        String sessionId = standardSession.getId();
        for (Session itSession : manager.findSessions()) {
            // disable update only other sessions - case: if I edit my account in /users -> changes will be see in /account
//            if(sessionId != null && sessionId.equals(itSession.getId()))
//                continue;
            HttpSession httpSession = itSession.getSession();
            if (httpSession == null)
                continue;
            SessionAccount sessionAccount = (SessionAccount) httpSession.getAttribute("sessionAccount");
            if (sessionAccount == null)
                continue;
            InternalAccount internalAccount = sessionAccount.getInternalAccount();
            if (internalAccount == null)
                continue;
            if (!internalAccount.getId().equals(sourceInternalAccount.getId()))
                continue;
            internalAccount.setName(sourceInternalAccount.getName());
            TreeSet<String> roles = convertRolesToNullIfEmpty(sourceInternalAccount);
            TreeSet<String> newRoles = null;
            if(roles != null) {
                newRoles = rolesContainerCreatorFunc.get();
                newRoles.addAll(roles);
            }
            internalAccount.setRoles(newRoles);
            convertRolesToNullIfEmpty(internalAccount);
            internalAccount.calcRoles();
            internalAccount.setIsBanned(sourceInternalAccount.getIsBanned());
            internalAccount.setBannedReason(sourceInternalAccount.getBannedReason());
            internalAccount.setGradeEatlog(sourceInternalAccount.getGradeEatlog());
            ++updatedCount;
            if(siteVisitorBySessionId != null) {
                String sessionId = httpSession.getId();
                if (sessionId != null) {
                    SiteVisitor siteVisitor = siteVisitorBySessionId.get(sessionId);
                    if (siteVisitor != null
                            && internalAccount.getId().equals(siteVisitor.getAccountId())) {
                        siteVisitor.setName(sourceInternalAccount.getName());
                        siteVisitor.setBanned(sourceInternalAccount.getIsBanned());
                        ++updatedCount;
                    }
                }
            }
        }
        return updatedCount;
    }

    public static String buildRangeStringFromSortedMapKeys(Map<?, ?> sortedMap,
                                                           String keyPrefix) {
        if(CollectionUtils.isEmpty(sortedMap))
            return "";
        return buildRangeStringFromSortedSet(sortedMap.keySet(), keyPrefix);
    }

    public static <T> String buildRangeStringFromSortedSet(Set<T> container,
                                                           String keyPrefix) {
        T from = CollectionUtils.firstElement(container);
        T to = CollectionUtils.lastElement(container);
        return buildRangeStringBundled(from, to, container, keyPrefix);
    }

//    public static <T> String buildRangeStringFromSortedList(List<T> sortedList,
//                                                        String keyPrefix) {
//        T from = CollectionUtils.firstElement(sortedList);
//        T to = CollectionUtils.lastElement(sortedList);
//        return buildRangeStringBundled(from, to, sortedList, keyPrefix);
//    }

//    public static String buildRangeStringFromUnsortedList(List<LocalDate> unsortedList,
//                                                          String keyPrefix) {
//        if(CollectionUtils.isEmpty(unsortedList)) {
//            return null;
//        }
//        LocalDate from = unsortedList.stream()
//                .min(LocalDate::compareTo)
//                .orElse(null);
//        LocalDate to = unsortedList.stream()
//                .max(LocalDate::compareTo)
//                .orElse(null);
//        return buildRangeStringBundled(from, to, unsortedList, keyPrefix);
//    }

    public static <T> String buildRangeStringBundled(
            T from,
            T to,
            Collection<?> container,
            String keyPrefix) {
        if(from == null || to == null) {
            return "";
        }
        LocalDate fromDate;
        LocalDate toDate;
        if(from instanceof LocalDate && to instanceof LocalDate) {
            fromDate = (LocalDate) from;
            toDate = (LocalDate) to;
        } else {
            fromDate = LocalDate.parse(from.toString(), DDMMYYYY_PATTERN);
            toDate = LocalDate.parse(to.toString(), DDMMYYYY_PATTERN);
        }
        return msgFromBundle("dates.from.to.days.records.counts",
                fromDate, toDate,
                humanLifetime(fromDate, toDate, true),
                declensionValuedL10N(container.size(), keyPrefix));
    }
}