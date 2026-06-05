package io.github.youngledo.jmcfx.ui.shell;

import java.util.Objects;

import org.kordamp.ikonli.Ikon;

record AppNavItem(String sectionId, String titleKey, Ikon icon, NavIconTone iconTone, AppWorkspaceKind workspaceKind,
        boolean group, boolean recordingScoped, boolean alwaysUnavailable) {

    AppNavItem {
        sectionId = sectionId == null ? "" : sectionId;
        titleKey = Objects.requireNonNull(titleKey, "titleKey");
        icon = Objects.requireNonNull(icon, "icon");
        iconTone = iconTone == null ? NavIconTone.NEUTRAL : iconTone;
        workspaceKind = workspaceKind == null ? AppWorkspaceKind.GLOBAL : workspaceKind;
    }

    static AppNavItem group(String titleKey, Ikon icon) {
        return group(titleKey, icon, NavIconTone.RECORDING);
    }

    static AppNavItem group(String titleKey, Ikon icon, NavIconTone iconTone) {
        return group("", titleKey, icon, iconTone);
    }

    static AppNavItem group(String sectionId, String titleKey, Ikon icon, NavIconTone iconTone) {
        return new AppNavItem(sectionId, titleKey, icon, iconTone, AppWorkspaceKind.GLOBAL, true, false, false);
    }

    static AppNavItem recordingPageGroup(String sectionId, String titleKey, Ikon icon, NavIconTone iconTone) {
        return new AppNavItem(sectionId, titleKey, icon, iconTone, AppWorkspaceKind.RECORDING, true, false, false);
    }

    static AppNavItem page(String sectionId, String titleKey, Ikon icon, boolean recordingScoped) {
        return page(sectionId, titleKey, icon, recordingScoped, NavIconTone.RECORDING);
    }

    static AppNavItem page(String sectionId, String titleKey, Ikon icon, boolean recordingScoped,
            NavIconTone iconTone) {
        AppWorkspaceKind workspaceKind = recordingScoped ? AppWorkspaceKind.RECORDING : AppWorkspaceKind.GLOBAL;
        return page(sectionId, titleKey, icon, workspaceKind, iconTone);
    }

    static AppNavItem page(String sectionId, String titleKey, Ikon icon, AppWorkspaceKind workspaceKind,
            NavIconTone iconTone) {
        return new AppNavItem(sectionId, titleKey, icon, iconTone, workspaceKind, false, false, false);
    }

    static AppNavItem unavailablePage(String sectionId, String titleKey, Ikon icon) {
        return new AppNavItem(sectionId, titleKey, icon, NavIconTone.NEUTRAL, AppWorkspaceKind.GLOBAL, false, false, true);
    }

    boolean page() {
        return !group || !sectionId.isBlank();
    }

    boolean unavailable(boolean recordingOpen) {
        return alwaysUnavailable || recordingScoped && !recordingOpen;
    }

    boolean visibleIn(AppWorkspaceKind activeWorkspaceKind) {
        return group && sectionId.isBlank()
                || workspaceKind == AppWorkspaceKind.GLOBAL
                || workspaceKind == activeWorkspaceKind;
    }
}
