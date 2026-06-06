package io.github.youngledo.jmcfx.domain.model;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record JmxNotificationListeningSummary(
        int listening,
        int stopped,
        int starting,
        int stopping,
        int failed,
        int unavailable) {

    public JmxNotificationListeningSummary {
        if (listening < 0 || stopped < 0 || starting < 0 || stopping < 0 || failed < 0 || unavailable < 0) {
            throw new IllegalArgumentException("Listening state counts must not be negative.");
        }
    }

    public static JmxNotificationListeningSummary from(Collection<JmxNotificationListeningState> states) {
        Objects.requireNonNull(states, "states");
        Map<JmxNotificationListeningState, Integer> counts = new EnumMap<>(JmxNotificationListeningState.class);
        for (JmxNotificationListeningState state : states) {
            counts.merge(Objects.requireNonNull(state, "state"), 1, Integer::sum);
        }
        return new JmxNotificationListeningSummary(
                counts.getOrDefault(JmxNotificationListeningState.LISTENING, 0),
                counts.getOrDefault(JmxNotificationListeningState.STOPPED, 0),
                counts.getOrDefault(JmxNotificationListeningState.STARTING, 0),
                counts.getOrDefault(JmxNotificationListeningState.STOPPING, 0),
                counts.getOrDefault(JmxNotificationListeningState.FAILED, 0),
                counts.getOrDefault(JmxNotificationListeningState.UNAVAILABLE, 0));
    }

    public int total() {
        return listening + stopped + starting + stopping + failed + unavailable;
    }
}
