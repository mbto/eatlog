package com.github.mbto.eatlog.common.utils;

import org.jooq.tools.json.ContainerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultContainerFactory implements ContainerFactory {
    public Map<?, ?> createObjectContainer() {
        return new LinkedHashMap<>();
    }

    public List<?> createArrayContainer() {
        return new ArrayList<>();
    }
}