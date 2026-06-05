package io.github.youngledo.jmcfx.domain.model;

public record DependencyGraphEdge(
        String source,
        String target,
        int count,
        double percentage) {

    public DependencyGraphEdge {
        source = normalizeLabel(source);
        target = normalizeLabel(target);
        count = Math.max(0, count);
        percentage = Math.max(0, percentage);
    }

    private static String normalizeLabel(String label) {
        return label == null || label.isBlank() ? "<unknown>" : label;
    }
}
