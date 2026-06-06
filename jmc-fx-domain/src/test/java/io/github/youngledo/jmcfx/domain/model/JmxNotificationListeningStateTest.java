package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class JmxNotificationListeningStateTest {

    @Test
    void definesRuntimeNotificationListeningStates() {
        assertEquals(List.of(
                JmxNotificationListeningState.LISTENING,
                JmxNotificationListeningState.STOPPED,
                JmxNotificationListeningState.STARTING,
                JmxNotificationListeningState.STOPPING,
                JmxNotificationListeningState.FAILED,
                JmxNotificationListeningState.UNAVAILABLE), List.of(JmxNotificationListeningState.values()));
    }

    @Test
    void summarizesRuntimeNotificationListeningStates() {
        JmxNotificationListeningSummary summary = JmxNotificationListeningSummary.from(List.of(
                JmxNotificationListeningState.LISTENING,
                JmxNotificationListeningState.STOPPED,
                JmxNotificationListeningState.STOPPED,
                JmxNotificationListeningState.STARTING,
                JmxNotificationListeningState.STOPPING,
                JmxNotificationListeningState.FAILED,
                JmxNotificationListeningState.UNAVAILABLE));

        assertEquals(1, summary.listening());
        assertEquals(2, summary.stopped());
        assertEquals(1, summary.starting());
        assertEquals(1, summary.stopping());
        assertEquals(1, summary.failed());
        assertEquals(1, summary.unavailable());
        assertEquals(7, summary.total());
    }
}
