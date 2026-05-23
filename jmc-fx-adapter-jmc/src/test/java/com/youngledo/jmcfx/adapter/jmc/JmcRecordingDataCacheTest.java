package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class JmcRecordingDataCacheTest {

    @Test
    void cacheDoesNotParseRecordingTwiceWhenBuildingItemCollection() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/youngledo/jmcfx/adapter/jmc/JmcRecordingDataCache.java"),
                StandardCharsets.UTF_8);

        assertFalse(source.contains("JfrLoaderToolkit.loadEvents(path.toFile())"));
    }
}
