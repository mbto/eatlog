package com.github.mbto.eatlog.utils;

import lombok.Getter;

import java.util.Map;

public class QueriesCache {
    @Getter
    private static final Map<String, String> queryByFilename;

    static {
        queryByFilename = ProjectUtils.collectResources("queries", "sql");
    }

    public static String get(String key) {
        return queryByFilename.get(key);
    }
}