package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import io.github.youngledo.jmcfx.domain.model.MBeanNode;
import io.github.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import io.github.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import io.github.youngledo.jmcfx.domain.model.MBeanOperationResult;

public interface MBeanBrowserService {
    default List<MBeanNode> tree(JvmConnection connection) {
        throw new JmcFxException("MBean browser is not supported by this service.");
    }

    default List<MBeanAttributeInfo> attributes(JvmConnection connection, String objectName) {
        throw new JmcFxException("MBean attributes are not supported by this service.");
    }

    default List<MBeanOperationInfo> operations(JvmConnection connection, String objectName) {
        throw new JmcFxException("MBean operations are not supported by this service.");
    }

    default MBeanOperationResult invoke(MBeanOperationRequest request) {
        throw new JmcFxException("MBean operation invocation is not supported by this service.");
    }
}
