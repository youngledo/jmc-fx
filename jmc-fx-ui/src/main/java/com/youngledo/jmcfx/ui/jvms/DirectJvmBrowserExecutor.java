package com.youngledo.jmcfx.ui.jvms;

public class DirectJvmBrowserExecutor implements JvmBrowserExecutor {
    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
