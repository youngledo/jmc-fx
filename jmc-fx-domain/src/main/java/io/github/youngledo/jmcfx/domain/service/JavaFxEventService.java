package io.github.youngledo.jmcfx.domain.service;

import io.github.youngledo.jmcfx.domain.model.JavaFxEventReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public interface JavaFxEventService {

    JavaFxEventReport loadJavaFxEvents(RecordingSummary recording);
}
