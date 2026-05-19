package com.youngledo.jmcfx.ui.shell;

import java.util.Objects;

import org.kordamp.ikonli.Ikon;

record AppNavItem(String sectionId, String titleKey, Ikon icon, boolean group, boolean recordingScoped,
        boolean alwaysUnavailable) {

    AppNavItem {
        sectionId = sectionId == null ? "" : sectionId;
        titleKey = Objects.requireNonNull(titleKey, "titleKey");
        icon = Objects.requireNonNull(icon, "icon");
    }

    static AppNavItem group(String titleKey, Ikon icon) {
        return new AppNavItem("", titleKey, icon, true, false, false);
    }

    static AppNavItem page(String sectionId, String titleKey, Ikon icon, boolean recordingScoped) {
        return new AppNavItem(sectionId, titleKey, icon, false, recordingScoped, false);
    }

    static AppNavItem unavailablePage(String sectionId, String titleKey, Ikon icon) {
        return new AppNavItem(sectionId, titleKey, icon, false, false, true);
    }

    boolean page() {
        return !group;
    }

    boolean unavailable(boolean recordingOpen) {
        return alwaysUnavailable || recordingScoped && !recordingOpen;
    }
}
