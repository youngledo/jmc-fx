package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import io.github.youngledo.jmcfx.domain.model.LiveMetricSnapshot;

public interface LiveMetricService {
    default List<LiveMetricDefinition> definitions(JvmConnection connection) {
        throw new JmcFxException("Live JVM metrics are not supported by this service.");
    }

    default List<LiveMetricSnapshot> snapshot(JvmConnection connection) {
        throw new JmcFxException("Live JVM metrics are not supported by this service.");
    }
}
