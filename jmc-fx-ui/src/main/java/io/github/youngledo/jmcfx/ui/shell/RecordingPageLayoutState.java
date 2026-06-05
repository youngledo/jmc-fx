package io.github.youngledo.jmcfx.ui.shell;

record RecordingPageLayoutState(String sectionId, double splitPosition, String selectedDetailTab) {

    RecordingPageLayoutState {
        if (sectionId == null || sectionId.isBlank()) {
            throw new IllegalArgumentException("sectionId must not be blank");
        }
        if (splitPosition <= 0 || splitPosition >= 1) {
            throw new IllegalArgumentException("splitPosition must be between 0 and 1");
        }
        selectedDetailTab = selectedDetailTab == null ? "" : selectedDetailTab;
    }

    RecordingPageLayoutState withSplitPosition(double splitPosition) {
        return new RecordingPageLayoutState(sectionId, splitPosition, selectedDetailTab);
    }

    RecordingPageLayoutState withSelectedDetailTab(String selectedDetailTab) {
        return new RecordingPageLayoutState(sectionId, splitPosition, selectedDetailTab);
    }
}
