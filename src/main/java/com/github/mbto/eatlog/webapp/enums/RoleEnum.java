package com.github.mbto.eatlog.webapp.enums;

import lombok.Getter;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum RoleEnum {
    /**
     * view/edit setting
     * view/edit other accounts
     * change roles in other accounts, except unownering self
     */
    OWNER("owner"),
    /**
     * view other accounts
     * edit name/is_banned/banned_reason other accounts, except owner
     */
    ADMIN("admin"),
    /**
     * edit recomendations
     */
    REDACTOR("redactor"),
    ;

    public static final Supplier<TreeSet<String>> rolesContainerCreatorFunc = TreeSet::new;

    public static final TreeSet<String> ALL_ROLES_NAMES = Stream.of(RoleEnum.values())
            .map(RoleEnum::getRoleName)
            .collect(Collectors.toCollection(rolesContainerCreatorFunc));

    @Getter
    private final String roleName;

    RoleEnum(String roleName) {
        this.roleName = roleName;
    }
}