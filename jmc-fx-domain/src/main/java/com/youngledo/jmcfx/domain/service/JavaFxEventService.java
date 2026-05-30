package com.youngledo.jmcfx.domain.service;

import com.youngledo.jmcfx.domain.model.JavaFxEventReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface JavaFxEventService {

    JavaFxEventReport loadJavaFxEvents(RecordingSummary recording);
}
