package com.github.mbto.eatlog.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SiteVisitors {
    private List<SiteVisitor> authedUsers;
    private int guestsCount;
}