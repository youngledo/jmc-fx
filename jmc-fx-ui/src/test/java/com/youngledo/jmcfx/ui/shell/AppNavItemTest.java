package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

class AppNavItemTest {

    @Test
    void groupItemsDoNotNavigate() {
        AppNavItem item = AppNavItem.group("nav.group.recording", Material2AL.ANALYTICS);

        assertEquals(true, item.group());
        assertEquals(false, item.page());
        assertEquals("", item.sectionId());
        assertEquals(false, item.recordingScoped());
        assertEquals(false, item.unavailable(false));
        assertEquals(NavIconTone.RECORDING, item.iconTone());
    }

    @Test
    void recordingScopedPagesAreUnavailableUntilRecordingOpens() {
        AppNavItem item = AppNavItem.page("analysis", "analysis.title", Material2AL.INSIGHTS, true);

        assertEquals(false, item.group());
        assertEquals(true, item.page());
        assertEquals("analysis", item.sectionId());
        assertEquals("analysis.title", item.titleKey());
        assertEquals(true, item.recordingScoped());
        assertEquals(true, item.unavailable(false));
        assertEquals(false, item.unavailable(true));
        assertEquals(NavIconTone.RECORDING, item.iconTone());
    }

    @Test
    void futurePagesCanBeMarkedUnavailable() {
        AppNavItem item = AppNavItem.unavailablePage("jvms", "jvms.title", Material2MZ.MEMORY);

        assertEquals(true, item.page());
        assertEquals(false, item.recordingScoped());
        assertEquals(true, item.unavailable(false));
        assertEquals(true, item.unavailable(true));
        assertEquals(NavIconTone.NEUTRAL, item.iconTone());
    }

    @Test
    void sectionItemsUseI18nTitleKeys() {
        assertEquals("overview.title", AppNavItem.page("overview", "overview.title",
                Material2MZ.PAGEVIEW, true).titleKey());
        assertEquals("events.title", AppNavItem.page("events", "events.title",
                Material2AL.EVENT, true).titleKey());
        assertEquals("analysis.title", AppNavItem.page("analysis", "analysis.title",
                Material2AL.INSIGHTS, true).titleKey());
        assertEquals("jvms.title", AppNavItem.unavailablePage("jvms", "jvms.title",
                Material2MZ.MEMORY).titleKey());
        assertEquals("settings.title", AppNavItem.page("settings", "settings.title",
                Material2MZ.SETTINGS, false).titleKey());
    }
}
