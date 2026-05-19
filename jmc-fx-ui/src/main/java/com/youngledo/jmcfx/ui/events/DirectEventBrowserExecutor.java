package com.youngledo.jmcfx.ui.events;

public class DirectEventBrowserExecutor implements EventBrowserBackgroundExecutor {
    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
