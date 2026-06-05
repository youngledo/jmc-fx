package io.github.youngledo.jmcfx.ui.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class LanguageModeTest {

    @Test
    void parsesPersistedValuesWithSystemFallback() {
        assertEquals(LanguageMode.ENGLISH, LanguageMode.fromPersistedValue("english"));
        assertEquals(LanguageMode.CHINESE_SIMPLIFIED, LanguageMode.fromPersistedValue("zh-CN"));
        assertEquals(LanguageMode.SYSTEM, LanguageMode.fromPersistedValue("system"));
        assertEquals(LanguageMode.SYSTEM, LanguageMode.fromPersistedValue(""));
        assertEquals(LanguageMode.SYSTEM, LanguageMode.fromPersistedValue("fr-FR"));
        assertEquals(LanguageMode.SYSTEM, LanguageMode.fromPersistedValue(null));
    }

    @Test
    void resolvesSupportedLocales() {
        assertEquals(Locale.ENGLISH, LanguageMode.ENGLISH.resolve(Locale.SIMPLIFIED_CHINESE));
        assertEquals(Locale.SIMPLIFIED_CHINESE, LanguageMode.CHINESE_SIMPLIFIED.resolve(Locale.ENGLISH));
        assertEquals(Locale.SIMPLIFIED_CHINESE, LanguageMode.SYSTEM.resolve(Locale.SIMPLIFIED_CHINESE));
        assertEquals(Locale.SIMPLIFIED_CHINESE, LanguageMode.SYSTEM.resolve(Locale.of("zh", "CN")));
        assertEquals(Locale.ENGLISH, LanguageMode.SYSTEM.resolve(Locale.of("fr", "FR")));
    }

    @Test
    void exposesPersistedValues() {
        assertEquals("english", LanguageMode.ENGLISH.persistedValue());
        assertEquals("zh-CN", LanguageMode.CHINESE_SIMPLIFIED.persistedValue());
        assertEquals("system", LanguageMode.SYSTEM.persistedValue());
    }
}
