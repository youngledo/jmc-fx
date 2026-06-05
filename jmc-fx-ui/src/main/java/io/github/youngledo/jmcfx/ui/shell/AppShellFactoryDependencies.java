package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.application.HeapDumpApplicationServices;
import io.github.youngledo.jmcfx.application.LiveJvmApplicationServices;
import io.github.youngledo.jmcfx.application.RecordingApplicationServices;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.preferences.AppPreferences;

public record AppShellFactoryDependencies(
        RecordingApplicationServices recordingServices,
        LiveJvmApplicationServices liveJvmServices,
        HeapDumpApplicationServices heapDumpServices,
        I18n i18n,
        AppPreferences preferences) {
}
