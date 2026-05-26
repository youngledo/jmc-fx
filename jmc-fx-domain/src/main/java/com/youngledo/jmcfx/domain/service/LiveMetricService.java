package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.LiveMetricSnapshot;

public interface LiveMetricService {
    default List<LiveMetricDefinition> definitions(JvmConnection connection) {
        throw new JmcFxException("Live JVM metrics are not supported by this service.");
    }

    default List<LiveMetricSnapshot> snapshot(JvmConnection connection) {
        throw new JmcFxException("Live JVM metrics are not supported by this service.");
    }
}
