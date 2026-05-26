package com.youngledo.jmcfx.domain.model;

public enum TriggerOperator {
    GREATER_THAN(">") {
        @Override
        public boolean test(double actual, double threshold) {
            return actual > threshold;
        }
    },
    GREATER_THAN_OR_EQUAL(">=") {
        @Override
        public boolean test(double actual, double threshold) {
            return actual >= threshold;
        }
    },
    LESS_THAN("<") {
        @Override
        public boolean test(double actual, double threshold) {
            return actual < threshold;
        }
    },
    LESS_THAN_OR_EQUAL("<=") {
        @Override
        public boolean test(double actual, double threshold) {
            return actual <= threshold;
        }
    };

    private final String symbol;

    TriggerOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public abstract boolean test(double actual, double threshold);
}
