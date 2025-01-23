package com.github.mbto.eatlog.common.custommodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mbto.eatlog.common.dto.CalcedRoles;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.primefaces.model.charts.line.LineChartModel;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.github.mbto.eatlog.webapp.WebUtils.buildRangeStringFromSortedMapKeys;
import static com.github.mbto.eatlog.webapp.WebUtils.generateLineChartModelWithStringLabels;
import static com.github.mbto.eatlog.webapp.enums.RoleEnum.*;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;

@ToString(callSuper = true)
public class InternalAccount extends Account {
    /* additional fields for custom queries: */

    @Getter
    @Setter
    private String weights_json;

    /* calced fields: */

    @Getter
    private final CalcedRoles calcedRoles = new CalcedRoles();
    @Getter
    private LineChartModel weightChart;

    public void calcRoles() {
        TreeSet<String> roles = getRoles();
        if(CollectionUtils.isEmpty(roles)) {
            calcedRoles.setOwner(false);
            calcedRoles.setAdmin(false);
            calcedRoles.setOwnerOrAdmin(false);
            calcedRoles.setRedactor(false);
            calcedRoles.setOwnerOrRedactor(false);
        } else {
            calcedRoles.setOwner(roles.contains(OWNER.getRoleName()));
            calcedRoles.setAdmin(roles.contains(ADMIN.getRoleName()));
            calcedRoles.setOwnerOrAdmin(calcedRoles.isOwner() || calcedRoles.isAdmin());
            calcedRoles.setRedactor(roles.contains(REDACTOR.getRoleName()));
            calcedRoles.setOwnerOrRedactor(calcedRoles.isOwner() || calcedRoles.isRedactor());
        }
    }

    public boolean hasRole(String candidate) {
        TreeSet<String> roles = getRoles();
        return roles != null && roles.contains(candidate);
    }

    public boolean hasAnyRole(TreeSet<String> candidates) {
        TreeSet<String> roles = getRoles();
        return CollectionUtils.containsAny(roles, candidates);
    }

    public boolean hasAnyRole(String... candidates) {
        if(candidates == null || candidates.length == 0)
            return false;
        TreeSet<String> roles = getRoles();
        if(CollectionUtils.isEmpty(roles))
            return false;
        for (String candidate : candidates) {
            if(roles.contains(candidate))
                return true;
        }
        return false;
    }

    public void setupWeightChart(ObjectMapper objectMapper) {
        if(weights_json == null) {
            weightChart = null;
            return;
        }
        Map<String, Double> weightByDateString;
        try {
            weightByDateString = objectMapper.readValue(weights_json,
                    new TypeReference<LinkedHashMap<String, Double>>() {});
        } catch (JsonProcessingException ignored) {
            weightChart = null;
            return;
        }
        if(weightByDateString.size() < 2) {
            weightChart = null;
            return;
        }
        weightChart = generateLineChartModelWithStringLabels(weightByDateString,
                getName(),
                buildRangeStringFromSortedMapKeys(weightByDateString, "record"));
    }

    @Override
    public Boolean getIsBanned() {
        return toBoolean(super.getIsBanned());
    }

    public static final Function<InternalAccount, TreeSet<String>> rolesGetterFunc = InternalAccount::getRoles;
    public static final BiConsumer<InternalAccount, TreeSet<String>> rolesSetterFunc = InternalAccount::setRoles;
    public static final Function<InternalAccount, Boolean> isBannedGetterFunc = InternalAccount::getIsBanned;
    public static final BiConsumer<InternalAccount, Boolean> isBannedSetterFunc = InternalAccount::setIsBanned;
}