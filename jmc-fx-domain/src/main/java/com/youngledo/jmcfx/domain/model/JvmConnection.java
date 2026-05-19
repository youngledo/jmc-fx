package com.youngledo.jmcfx.domain.model;

public record JvmConnection(
        String id,
        String displayName,
        String connectionUrl,
        boolean connected) {
}
