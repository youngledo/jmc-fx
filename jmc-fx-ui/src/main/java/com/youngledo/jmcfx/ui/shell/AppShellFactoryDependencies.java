package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.application.HeapDumpApplicationServices;
import com.youngledo.jmcfx.application.LiveJvmApplicationServices;
import com.youngledo.jmcfx.application.RecordingApplicationServices;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.preferences.AppPreferences;

public record AppShellFactoryDependencies(
        RecordingApplicationServices recordingServices,
        LiveJvmApplicationServices liveJvmServices,
        HeapDumpApplicationServices heapDumpServices,
        I18n i18n,
        AppPreferences preferences) {
}
