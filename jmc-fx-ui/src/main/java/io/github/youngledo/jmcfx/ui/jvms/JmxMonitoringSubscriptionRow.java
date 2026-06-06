package io.github.youngledo.jmcfx.ui.jvms;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationListeningState;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationSubscription;

public record JmxMonitoringSubscriptionRow(
        Kind kind,
        String label,
        String target,
        String detail,
        JmxNotificationListeningState listeningState,
        JmxAttributeSubscription attributeSubscription,
        JmxNotificationSubscription notificationSubscription) {

    public JmxMonitoringSubscriptionRow {
        Objects.requireNonNull(kind, "kind");
        label = Objects.requireNonNullElse(label, "");
        target = Objects.requireNonNullElse(target, "");
        detail = Objects.requireNonNullElse(detail, "");
    }

    public static JmxMonitoringSubscriptionRow attribute(JmxAttributeSubscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        return new JmxMonitoringSubscriptionRow(
                Kind.ATTRIBUTE,
                subscription.label(),
                subscription.objectName() + " / " + subscription.attributeName(),
                subscription.samplingInterval().toSeconds() + "s",
                null,
                subscription,
                null);
    }

    public static JmxMonitoringSubscriptionRow notification(JmxNotificationSubscription subscription) {
        return notification(subscription, JmxNotificationListeningState.STOPPED);
    }

    public static JmxMonitoringSubscriptionRow notification(
            JmxNotificationSubscription subscription, JmxNotificationListeningState listeningState) {
        Objects.requireNonNull(subscription, "subscription");
        return new JmxMonitoringSubscriptionRow(
                Kind.NOTIFICATION,
                subscription.label(),
                subscription.objectName(),
                Integer.toString(subscription.maxEvents()),
                Objects.requireNonNullElse(listeningState, JmxNotificationListeningState.UNAVAILABLE),
                null,
                subscription);
    }

    public enum Kind {
        ATTRIBUTE,
        NOTIFICATION
    }
}
