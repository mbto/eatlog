package com.github.mbto.eatlog.common;

import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Setting;
import org.jooq.Param;
import org.jooq.impl.DSL;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Constants {
    DateTimeFormatter YYYYMMDD_HHMMSS_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter MMDDYYYY_HHMMSS_PATTERN = DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss");
    DateTimeFormatter DDMMYYYY_HHMMSS_PATTERN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    DateTimeFormatter DDMMYYYY_HHMM_PATTERN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    DateTimeFormatter YYYYMMDD_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter DDMMYYYY_PATTERN = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    String BUNDLE_PROPS_SIMPLE_DECLENSION_ENABLED = "language.simple.declension.enabled";

    String OBSERVE_USER_REQUEST_PARAM = "id";
    String LOGOUT_REQUEST_PARAM = "logout";
    String STATE_REQUEST_PARAM = "state";
    String CODE_REQUEST_PARAM = "code";

    List<String> CARRY_REQUEST_PARAMS = List.of(
            OBSERVE_USER_REQUEST_PARAM, LOGOUT_REQUEST_PARAM);

    String hideEditChildDialogWidget = "PF('editChildDialogW').hide()";
    String editChildsForm_childsTable = "editChildsForm:childsTable";
    String userStateMsgs = "userStateMsgs";
    String dialogMsgs = "dialogMsgs";
    String msgs = "msgs";

    String GEO_SCHEMA_NAME = "eatlog_maxmind_city";

    List<Param<String>> GEO_TABLES_NAMES = Stream.of(
            "city", "country", "ipv4", "subdivision1", "subdivision2")
            .map(DSL::val)
            .toList();

    Function<Setting, TreeSet<String>> settingRolesGetterFunc = Setting::getRoles;
    BiConsumer<Setting, TreeSet<String>> settingRolesSetterFunc = Setting::setRoles;
}