package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record TriggerRule(
        String id,
        String name,
        boolean enabled,
        LiveMetricKind metric,
        TriggerOperator operator,
        double threshold,
        TriggerAction action) {

    public TriggerRule {
        id = Objects.requireNonNullElse(id, "");
        name = Objects.requireNonNullElse(name, "");
        metric = Objects.requireNonNull(metric, "metric");
        operator = Objects.requireNonNull(operator, "operator");
        action = Objects.requireNonNull(action, "action");
    }

    public boolean matches(LiveMetricSnapshot snapshot) {
        return enabled
                && snapshot != null
                && snapshot.kind() == metric
                && operator.test(snapshot.value(), threshold);
    }
}
