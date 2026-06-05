package io.github.youngledo.jmcfx.launcher;

import io.github.youngledo.jmcfx.application.HeapDumpApplicationServices;
import io.github.youngledo.jmcfx.application.LiveJvmApplicationServices;
import io.github.youngledo.jmcfx.application.RecordingApplicationServices;

record JmcFxLauncherServices(
        RecordingApplicationServices recording,
        LiveJvmApplicationServices liveJvm,
        HeapDumpApplicationServices heapDump) {
}
