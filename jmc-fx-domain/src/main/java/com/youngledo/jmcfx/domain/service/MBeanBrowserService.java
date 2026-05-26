package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;

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
