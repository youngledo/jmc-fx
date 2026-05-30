package com.youngledo.jmcfx.ui.jvms;

public record LiveJvmPersistenceOverview(
        boolean configured,
        int attributeSubscriptions,
        int persistedAttributeSubscriptions,
        int notificationSubscriptions,
        int persistedNotificationSubscriptions,
        int maxSamples,
        int maxEvents) {

    public static LiveJvmPersistenceOverview notConfigured() {
        return new LiveJvmPersistenceOverview(false, 0, 0, 0, 0, 0, 0);
    }

    public boolean empty() {
        return configured
                && attributeSubscriptions == 0
                && persistedAttributeSubscriptions == 0
                && notificationSubscriptions == 0
                && persistedNotificationSubscriptions == 0
                && maxSamples == 0
                && maxEvents == 0;
    }
}
