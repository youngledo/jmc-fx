package com.youngledo.jmcfx.application;

import java.util.List;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;
import com.youngledo.jmcfx.domain.service.MBeanBrowserService;

public final class LiveJvmMBeanUseCase {

    private final MBeanBrowserService service;

    public LiveJvmMBeanUseCase(MBeanBrowserService service) {
        this.service = service;
    }

    public boolean available() {
        return service != null;
    }

    public List<MBeanNode> tree(JvmConnection connection) {
        return service.tree(connection);
    }

    public List<MBeanAttributeInfo> attributes(JvmConnection connection, String objectName) {
        return service.attributes(connection, objectName);
    }

    public List<MBeanOperationInfo> operations(JvmConnection connection, String objectName) {
        return service.operations(connection, objectName);
    }

    public MBeanOperationResult invoke(MBeanOperationRequest request) {
        return service.invoke(request);
    }
}
