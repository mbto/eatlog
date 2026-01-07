package com.github.mbto.eatlog.common;

import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Setting;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface Constants {
    DateTimeFormatter YYYYMMDD_HHMMSS_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter MMDDYYYY_HHMMSS_PATTERN = DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss");
    DateTimeFormatter DDMMYYYY_HHMMSS_PATTERN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    DateTimeFormatter DDMMYYYY_HHMM_PATTERN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    DateTimeFormatter YYYYMMDD_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter DDMMYYYY_PATTERN = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    String BUNDLE_PROPS_SIMPLE_DECLENSION_ENABLED = "language.simple.declension.enabled";

    String ACCOUNT_PAGE = "/account";

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

    String RU_LOCALE_LANGUAGE = Locale.of("ru").getLanguage();

    Function<Setting, TreeSet<String>> settingRolesGetterFunc = Setting::getRoles;
    BiConsumer<Setting, TreeSet<String>> settingRolesSetterFunc = Setting::setRoles;
}