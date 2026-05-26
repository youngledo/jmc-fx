package com.youngledo.jmcfx.testsupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.MBeanBrowserService;

public class FakeMBeanBrowserService implements MBeanBrowserService {

    private final Map<String, List<MBeanNode>> trees = new HashMap<>();
    private final Map<Key, List<MBeanAttributeInfo>> attributes = new HashMap<>();
    private final Map<Key, List<MBeanOperationInfo>> operations = new HashMap<>();
    private final Map<OperationKey, MBeanOperationResult> results = new HashMap<>();
    private RuntimeException failure;

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public void setTree(String connectionId, List<MBeanNode> tree) {
        trees.put(connectionId, List.copyOf(tree));
    }

    public void setAttributes(String connectionId, String objectName, List<MBeanAttributeInfo> rows) {
        attributes.put(new Key(connectionId, objectName), List.copyOf(rows));
    }

    public void setOperations(String connectionId, String objectName, List<MBeanOperationInfo> rows) {
        operations.put(new Key(connectionId, objectName), List.copyOf(rows));
    }

    public void setOperationResult(String connectionId, String objectName, String operationName,
            MBeanOperationResult result) {
        results.put(new OperationKey(connectionId, objectName, operationName), result);
    }

    @Override
    public List<MBeanNode> tree(JvmConnection connection) {
        failIfConfigured();
        return List.copyOf(trees.getOrDefault(connectionId(connection), List.of()));
    }

    @Override
    public List<MBeanAttributeInfo> attributes(JvmConnection connection, String objectName) {
        failIfConfigured();
        Key key = new Key(connectionId(connection), objectName);
        List<MBeanAttributeInfo> rows = attributes.get(key);
        if (rows == null) {
            throw new JmcFxException("No fake MBean attributes for " + key.connectionId() + " " + key.objectName());
        }
        return List.copyOf(rows);
    }

    @Override
    public List<MBeanOperationInfo> operations(JvmConnection connection, String objectName) {
        failIfConfigured();
        return List.copyOf(operations.getOrDefault(new Key(connectionId(connection), objectName), List.of()));
    }

    @Override
    public MBeanOperationResult invoke(MBeanOperationRequest request) {
        failIfConfigured();
        OperationKey key = new OperationKey(request.connection().id(), request.objectName(), request.operationName());
        MBeanOperationResult result = results.get(key);
        if (result == null) {
            throw new JmcFxException("No fake MBean operation result for " + key.operationName());
        }
        return result;
    }

    private void failIfConfigured() {
        if (failure != null) {
            throw failure;
        }
    }

    private static String connectionId(JvmConnection connection) {
        return connection == null ? "" : connection.id();
    }

    private record Key(String connectionId, String objectName) {
        private Key {
            connectionId = Objects.requireNonNullElse(connectionId, "");
            objectName = Objects.requireNonNullElse(objectName, "");
        }
    }

    private record OperationKey(String connectionId, String objectName, String operationName) {
        private OperationKey {
            connectionId = Objects.requireNonNullElse(connectionId, "");
            objectName = Objects.requireNonNullElse(objectName, "");
            operationName = Objects.requireNonNullElse(operationName, "");
        }
    }
}
