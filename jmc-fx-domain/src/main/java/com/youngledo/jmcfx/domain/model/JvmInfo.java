package com.youngledo.jmcfx.domain.model;

public record JvmInfo(
        String jvmName,
        String jvmVersion,
        String jvmArguments,
        long pid) {
}
