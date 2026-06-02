package com.youngledo.jmcfx.domain.model;

public record StackFrameInfo(
        String label,
        String methodName,
        String packageName,
        String typeName,
        String frameType,
        Integer bci,
        Integer lineNumber) {

    public static final StackFrameInfo EMPTY = new StackFrameInfo("", "", "", "", "", null, null);

    public StackFrameInfo {
        label = label == null ? "" : label;
        methodName = methodName == null ? "" : methodName;
        packageName = packageName == null ? "" : packageName;
        typeName = typeName == null ? "" : typeName;
        frameType = frameType == null ? "" : frameType;
    }

    public boolean hasDetails() {
        return !label.isBlank()
                || !methodName.isBlank()
                || !packageName.isBlank()
                || !typeName.isBlank()
                || !frameType.isBlank()
                || bci != null
                || lineNumber != null;
    }
}
