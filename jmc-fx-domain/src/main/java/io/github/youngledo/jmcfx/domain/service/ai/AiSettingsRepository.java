package io.github.youngledo.jmcfx.domain.service.ai;

import java.util.Optional;

import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;

public interface AiSettingsRepository {
    Optional<AiSettings> load();

    void save(AiSettings settings);
}
