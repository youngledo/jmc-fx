package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public record JmxNotificationHistoryFilter(
        String typeContains,
        String messageContains,
        Instant from,
        Instant to) {

    public JmxNotificationHistoryFilter {
        typeContains = normalize(typeContains);
        messageContains = normalize(messageContains);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }

    public boolean matches(JmxNotificationEvent event) {
        Objects.requireNonNull(event, "event");
        return contains(event.type(), typeContains)
                && contains(event.message(), messageContains)
                && (from == null || !event.observedAt().isBefore(from))
                && (to == null || !event.observedAt().isAfter(to));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String value, String filter) {
        return filter.isBlank()
                || Objects.requireNonNullElse(value, "")
                        .toLowerCase(Locale.ROOT)
                        .contains(filter);
    }
}
