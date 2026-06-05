package io.github.youngledo.jmcfx.domain.service;

import java.time.Instant;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.SavedJvmTarget;

public interface SavedJvmTargetRepository {

    List<SavedJvmTarget> findAll();

    SavedJvmTarget save(SavedJvmTarget target);

    void deleteById(String id);

    void markConnected(String id, Instant connectedAt);
}
