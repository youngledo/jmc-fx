package com.youngledo.jmcfx.domain.model;

import java.util.List;

/// UI-neutral node in the JMC-compatible event type tree.
public record EventTypeNode(
        String id,
        String label,
        EventTypeNodeKind kind,
        List<String> categoryPath,
        String eventTypeId,
        long count,
        List<EventTypeNode> children) {

    public EventTypeNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        eventTypeId = eventTypeId == null ? "" : eventTypeId;
        categoryPath = List.copyOf(categoryPath);
        children = List.copyOf(children);
        if (kind == EventTypeNodeKind.GROUP && !eventTypeId.isBlank()) {
            throw new IllegalArgumentException("group nodes must not have an event type id");
        }
        if (kind == EventTypeNodeKind.EVENT_TYPE) {
            if (eventTypeId.isBlank()) {
                throw new IllegalArgumentException("event type nodes must have an event type id");
            }
            if (count < 0) {
                throw new IllegalArgumentException("event type node count must be >= 0");
            }
            if (!children.isEmpty()) {
                throw new IllegalArgumentException("event type nodes must not have children");
            }
        }
        if (count < -1) {
            throw new IllegalArgumentException("count must be >= -1");
        }
    }

    public static EventTypeNode group(String id, String label, List<String> categoryPath, List<EventTypeNode> children) {
        return group(id, label, categoryPath, children.stream()
                .mapToLong(EventTypeNode::count)
                .filter(count -> count >= 0)
                .sum(), children);
    }

    public static EventTypeNode group(String id, String label, List<String> categoryPath, long count,
            List<EventTypeNode> children) {
        return new EventTypeNode(id, label, EventTypeNodeKind.GROUP, categoryPath, "", count, children);
    }

    public static EventTypeNode eventType(String eventTypeId, String label, List<String> categoryPath, long count) {
        return new EventTypeNode(eventTypeId, label, EventTypeNodeKind.EVENT_TYPE, categoryPath, eventTypeId, count,
                List.of());
    }
}
