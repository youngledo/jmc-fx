package io.github.youngledo.jmcfx.domain.model;

import java.util.List;

public record ConstantPoolType(
        String typeName,
        String typeId,
        int entryCount,
        List<ConstantPoolEntry> entries) {
}
