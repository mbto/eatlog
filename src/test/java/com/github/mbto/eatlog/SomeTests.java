package com.github.mbto.eatlog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

public class SomeTests {
    @Test
    public void test() {
        Locale ru0 = Locale.of("RU");
        Locale ru1 = Locale.of("ru");
        Locale ru2 = Locale.of("ru_RU");
        Assertions.assertEquals(ru0, ru1);
        Assertions.assertNotEquals(ru1, ru2);
    }
}