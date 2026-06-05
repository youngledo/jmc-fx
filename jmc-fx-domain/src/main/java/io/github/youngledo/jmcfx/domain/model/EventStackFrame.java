package io.github.youngledo.jmcfx.domain.model;

public record EventStackFrame(String typeName, String methodName, String fileName, int lineNumber) {
}
