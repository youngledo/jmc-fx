package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.preferences.AppPreferences;

public record AppShellFactoryDependencies(
        RecordingServices recordingServices,
        LiveJvmServices liveJvmServices,
        HeapDumpServices heapDumpServices,
        I18n i18n,
        AppPreferences preferences) {
}
