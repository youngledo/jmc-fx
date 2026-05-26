package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record MBeanNode(String name, String objectName, boolean domain, List<MBeanNode> children) {

    public MBeanNode {
        name = Objects.requireNonNullElse(name, "");
        objectName = Objects.requireNonNullElse(objectName, "");
        children = List.copyOf(Objects.requireNonNullElse(children, List.of()));
    }

    public static MBeanNode domain(String name, List<MBeanNode> children) {
        return new MBeanNode(name, "", true, children);
    }

    public static MBeanNode objectName(String objectName, String displayName) {
        return new MBeanNode(displayName, objectName, false, List.of());
    }
}
