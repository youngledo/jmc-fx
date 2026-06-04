package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;

import javax.management.MBeanServerConnection;

import com.youngledo.jmcfx.domain.model.JvmConnection;

@FunctionalInterface
interface JmxConnectionAccessor {
    MBeanServerConnection mBeanServerConnection(JvmConnection connection) throws IOException;
}
