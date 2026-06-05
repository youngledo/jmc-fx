package io.github.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;

import javax.management.MBeanServerConnection;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;

@FunctionalInterface
interface JmxConnectionAccessor {
    MBeanServerConnection mBeanServerConnection(JvmConnection connection) throws IOException;
}
