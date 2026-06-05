package io.github.youngledo.jmcfx.domain.model;

/// Categorizes the type of activity a thread is performing in a time lane.
///
/// Each lane type corresponds to a category of JFR events shown on the
/// thread activity timeline.
public enum ThreadLaneType {
	CPU_SAMPLE("CPU Sample"),
	BLOCKED("Blocked"),
	PARKED("Parked"),
	SLEEPING("Sleeping"),
	SOCKET_IO("Socket I/O"),
	FILE_IO("File I/O"),
	COMPILATION("Compilation"),
	CLASS_LOAD("Class Load");

	private final String displayName;

	ThreadLaneType(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}
}
