package io.github.youngledo.jmcfx.ui.shell;

record GlobalWorkspaceTab(String sectionId, String titleKey) {

    static final GlobalWorkspaceTab HOME = new GlobalWorkspaceTab("home", "nav.home");
    static final GlobalWorkspaceTab SETTINGS = new GlobalWorkspaceTab("settings", "settings.title");

    static GlobalWorkspaceTab forSection(String sectionId) {
        return switch (sectionId) {
            case "home" -> HOME;
            case "settings" -> SETTINGS;
            default -> throw new IllegalArgumentException("Unsupported global tab section: " + sectionId);
        };
    }
}
