package com.youngledo.jmcfx.launcher;

import com.youngledo.jmcfx.application.HeapDumpApplicationServices;
import com.youngledo.jmcfx.application.LiveJvmApplicationServices;
import com.youngledo.jmcfx.application.RecordingApplicationServices;

record JmcApplicationServices(
        RecordingApplicationServices recording,
        LiveJvmApplicationServices liveJvm,
        HeapDumpApplicationServices heapDump) {
}
