package io.github.youngledo.jmcfx.ui.preferences;

import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;

public interface AppPreferences {

    LanguageMode languageMode();

    void setLanguageMode(LanguageMode mode);

    AppTheme theme();

    void setTheme(AppTheme theme);
}
