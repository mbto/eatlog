package com.github.mbto.eatlog.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CalcedRoles {
    private boolean isOwner;
    private boolean isAdmin;
    private boolean isOwnerOrAdmin;
    private boolean isRedactor;
    private boolean isOwnerOrRedactor;
}