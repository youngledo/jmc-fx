package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class MBeanBrowserModelTest {

    @Test
    void mbeanNodeNormalizesTextAndCopiesChildren() {
        MBeanNode child = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanNode node = MBeanNode.domain(null, List.of(child));

        assertEquals("", node.name());
        assertEquals("", node.objectName());
        assertEquals(1, node.children().size());
        assertThrows(UnsupportedOperationException.class,
                () -> node.children().add(MBeanNode.domain("x", List.of())));
    }

    @Test
    void attributeInfoNormalizesValues() {
        MBeanAttributeInfo attribute = new MBeanAttributeInfo(null, null, true, false, null, null);

        assertEquals("", attribute.name());
        assertEquals("", attribute.type());
        assertEquals("", attribute.value());
        assertEquals("", attribute.error());
    }

    @Test
    void operationInfoCopiesParameters() {
        MBeanOperationInfo operation = new MBeanOperationInfo("gc", "void", null,
                List.of(new MBeanOperationParameter("verbose", "boolean", null)));

        assertEquals("gc", operation.name());
        assertEquals("void", operation.returnType());
        assertEquals("", operation.description());
        assertEquals("boolean", operation.parameters().getFirst().type());
        assertThrows(UnsupportedOperationException.class,
                () -> operation.parameters().add(new MBeanOperationParameter("x", "java.lang.String", "")));
    }

    @Test
    void operationRequestRequiresConnection() {
        assertThrows(NullPointerException.class,
                () -> new MBeanOperationRequest(null, "java.lang:type=Memory", "gc", List.of()));
    }

    @Test
    void operationResultDefaultsSuccessAndNormalizesText() {
        MBeanOperationResult result = new MBeanOperationResult(true, null, null);

        assertEquals("", result.value());
        assertEquals("", result.error());
    }
}
