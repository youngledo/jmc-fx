package com.youngledo.jmcfx.domain.service;

public class JmcFxException extends RuntimeException {

    public JmcFxException(String message, Throwable cause) {
        super(message, cause);
    }

    public JmcFxException(String message) {
        super(message);
    }
}
