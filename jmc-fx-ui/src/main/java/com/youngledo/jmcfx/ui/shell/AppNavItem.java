package com.youngledo.jmcfx.ui.shell;

import java.util.Objects;

import org.kordamp.ikonli.Ikon;

record AppNavItem(String sectionId, String titleKey, Ikon icon, NavIconTone iconTone, boolean group, boolean recordingScoped,
        boolean alwaysUnavailable) {

    AppNavItem {
        sectionId = sectionId == null ? "" : sectionId;
        titleKey = Objects.requireNonNull(titleKey, "titleKey");
        icon = Objects.requireNonNull(icon, "icon");
        iconTone = iconTone == null ? NavIconTone.NEUTRAL : iconTone;
    }

    static AppNavItem group(String titleKey, Ikon icon) {
        return group(titleKey, icon, NavIconTone.RECORDING);
    }

    static AppNavItem group(String titleKey, Ikon icon, NavIconTone iconTone) {
        return new AppNavItem("", titleKey, icon, iconTone, true, false, false);
    }

    static AppNavItem page(String sectionId, String titleKey, Ikon icon, boolean recordingScoped) {
        return page(sectionId, titleKey, icon, recordingScoped, NavIconTone.RECORDING);
    }

    static AppNavItem page(String sectionId, String titleKey, Ikon icon, boolean recordingScoped,
            NavIconTone iconTone) {
        return new AppNavItem(sectionId, titleKey, icon, iconTone, false, recordingScoped, false);
    }

    static AppNavItem unavailablePage(String sectionId, String titleKey, Ikon icon) {
        return new AppNavItem(sectionId, titleKey, icon, NavIconTone.NEUTRAL, false, false, true);
    }

    boolean page() {
        return !group;
    }

    boolean unavailable(boolean recordingOpen) {
        return alwaysUnavailable || recordingScoped && !recordingOpen;
    }
}
