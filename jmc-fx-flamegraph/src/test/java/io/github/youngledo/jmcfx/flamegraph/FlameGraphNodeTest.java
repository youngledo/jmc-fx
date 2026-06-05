package io.github.youngledo.jmcfx.flamegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class FlameGraphNodeTest {

    @Test
    void normalizesNullLabelNegativeWeightAndNullChildren() {
        FlameGraphNode<String> node = new FlameGraphNode<>(null, -4, 12.5, "payload", null);

        assertEquals("", node.label());
        assertEquals(0, node.weight());
        assertEquals(12.5, node.percentage());
        assertEquals("payload", node.payload());
        assertTrue(node.children().isEmpty());
    }

    @Test
    void preservesChildren() {
        FlameGraphNode<String> child = new FlameGraphNode<>("child", 10, 10, "c", List.of());
        FlameGraphNode<String> root = new FlameGraphNode<>("root", 10, 100, "r", List.of(child));

        assertEquals(List.of(child), root.children());
    }
}
