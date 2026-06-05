package io.github.youngledo.jmcfx.ui.i18n;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Tooltip;

public final class I18n {

    private static final String BUNDLE_BASE_NAME = "io.github.youngledo.jmcfx.ui.i18n.messages";

    private final Locale systemLocale;
    private final ObjectProperty<LanguageMode> languageMode = new SimpleObjectProperty<>(LanguageMode.ENGLISH);
    private final ReadOnlyObjectWrapper<Locale> locale = new ReadOnlyObjectWrapper<>(Locale.ENGLISH);

    public I18n(Locale systemLocale) {
        this.systemLocale = systemLocale == null ? Locale.getDefault() : systemLocale;
        languageMode.addListener((observable, oldValue, newValue) -> updateLocale());
        updateLocale();
    }

    public ObjectProperty<LanguageMode> languageModeProperty() {
        return languageMode;
    }

    public void setLanguageMode(LanguageMode mode) {
        languageMode.set(mode == null ? LanguageMode.ENGLISH : mode);
    }

    public ReadOnlyObjectProperty<Locale> localeProperty() {
        return locale.getReadOnlyProperty();
    }

    public String get(String key) {
        try {
            return bundle().getString(key);
        } catch (MissingResourceException exception) {
            return "!" + key + "!";
        }
    }

    public String format(String key, Object... arguments) {
        return new MessageFormat(get(key), locale.get()).format(arguments);
    }

    public StringBinding text(String key, Object... arguments) {
        return Bindings.createStringBinding(() -> format(key, arguments), locale);
    }

    public Tooltip tooltip(String key, Object... arguments) {
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(text(key, arguments));
        return tooltip;
    }

    private void updateLocale() {
        locale.set(languageMode.get().resolve(systemLocale));
    }

    private ResourceBundle bundle() {
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, bundleLocale(locale.get()));
    }

    static List<Locale> candidateLocales(Locale locale) {
        Locale bundleLocale = bundleLocale(locale);
        if (bundleLocale.equals(Locale.ROOT)) {
            return List.of(Locale.ROOT);
        }
        return List.of(bundleLocale, Locale.ROOT);
    }

    private static Locale bundleLocale(Locale locale) {
        if (Locale.SIMPLIFIED_CHINESE.equals(locale)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.ROOT;
    }
}
