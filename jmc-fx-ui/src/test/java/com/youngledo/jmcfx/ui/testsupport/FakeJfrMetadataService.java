package com.youngledo.jmcfx.ui.testsupport;

import java.util.List;

import com.youngledo.jmcfx.domain.model.EventValueType;
import com.youngledo.jmcfx.domain.model.JfrMetadataEventType;
import com.youngledo.jmcfx.domain.model.JfrMetadataField;
import com.youngledo.jmcfx.domain.model.JfrMetadataReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JfrMetadataService;

public class FakeJfrMetadataService implements JfrMetadataService {

    @Override
    public JfrMetadataReport loadMetadata(RecordingSummary recording) {
        return new JfrMetadataReport(List.of(
                new JfrMetadataEventType("jdk.CPULoad", "CPU Load", List.of("Operating System"), 1,
                        "CPU load sampled by JFR.",
                        List.of(new JfrMetadataField("jvmUser", "JVM User", "JVM user CPU load.",
                                EventValueType.NUMBER, "%"))),
                new JfrMetadataEventType("jdk.ThreadStart", "Thread Start", List.of("Java Application"), 2,
                        "Thread start events.",
                        List.of(new JfrMetadataField("eventThread", "Event Thread", "Thread for the event.",
                                EventValueType.TEXT, "")))));
    }
}
