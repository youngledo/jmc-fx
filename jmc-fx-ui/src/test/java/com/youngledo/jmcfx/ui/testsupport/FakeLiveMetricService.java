package com.youngledo.jmcfx.ui.testsupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.LiveMetricSnapshot;
import com.youngledo.jmcfx.domain.service.LiveMetricService;

public class FakeLiveMetricService implements LiveMetricService {

    private final Map<String, List<LiveMetricDefinition>> definitions = new HashMap<>();
    private final Map<String, List<LiveMetricSnapshot>> snapshots = new HashMap<>();
    private RuntimeException failure;

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public void setDefinitions(String connectionId, List<LiveMetricDefinition> rows) {
        definitions.put(Objects.requireNonNullElse(connectionId, ""), List.copyOf(rows));
    }

    public void setSnapshot(String connectionId, List<LiveMetricSnapshot> rows) {
        snapshots.put(Objects.requireNonNullElse(connectionId, ""), List.copyOf(rows));
    }

    @Override
    public List<LiveMetricDefinition> definitions(JvmConnection connection) {
        failIfConfigured();
        return List.copyOf(definitions.getOrDefault(connectionId(connection), List.of()));
    }

    @Override
    public List<LiveMetricSnapshot> snapshot(JvmConnection connection) {
        failIfConfigured();
        return List.copyOf(snapshots.getOrDefault(connectionId(connection), List.of()));
    }

    private void failIfConfigured() {
        if (failure != null) {
            throw failure;
        }
    }

    private static String connectionId(JvmConnection connection) {
        return connection == null ? "" : connection.id();
    }
}
