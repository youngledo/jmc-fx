package com.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Instant;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.RecordingRepository;
import org.junit.jupiter.api.Test;

class OpenRecordingWorkspaceUseCaseTest {

    @Test
    void opensRecordingAndReportsAvailableWorkspaceCapabilities() {
        Path path = Path.of("startup.jfr");
        RecordingSummary recording = new RecordingSummary("recording-1", path, "startup.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
        RecordingRepository repository = requestedPath -> {
            assertEquals(path, requestedPath);
            return recording;
        };
        RecordingApplicationServices services = new RecordingApplicationServices(repository,
                stub(com.youngledo.jmcfx.domain.service.EventQueryService.class),
                stub(com.youngledo.jmcfx.domain.service.RuleAnalysisService.class),
                stub(com.youngledo.jmcfx.domain.service.ProfilingService.class),
                null,
                stub(com.youngledo.jmcfx.domain.service.ThreadService.class),
                null, null, null, null, null, null,
                stub(com.youngledo.jmcfx.domain.service.JvmInternalsService.class),
                null,
                stub(com.youngledo.jmcfx.domain.service.JavaAppService.class),
                null, null, null,
                stub(com.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService.class));

        OpenRecordingWorkspaceUseCase useCase = new OpenRecordingWorkspaceUseCase(services);

        RecordingWorkspacePlan plan = useCase.open(path);

        assertEquals(recording, plan.recording());
        assertTrue(plan.hasProfiling());
        assertFalse(plan.hasExceptions());
        assertTrue(plan.hasThreads());
        assertFalse(plan.hasFileIO());
        assertTrue(plan.hasJvmInternals());
        assertTrue(plan.hasJavaApplication());
        assertFalse(plan.hasMetadata());
        assertTrue(plan.hasAdvancedJfrAnalysis());
    }

    private static <T> T stub(Class<T> type) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (ignored, method, ignoredArgs) -> throwUnsupported(method.getName()));
        return type.cast(proxy);
    }

    private static Object throwUnsupported(String methodName) {
        throw new UnsupportedOperationException(methodName);
    }
}
