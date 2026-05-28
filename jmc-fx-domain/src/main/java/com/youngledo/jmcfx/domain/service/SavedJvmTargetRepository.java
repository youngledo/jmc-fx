package com.youngledo.jmcfx.domain.service;

import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.SavedJvmTarget;

public interface SavedJvmTargetRepository {

    List<SavedJvmTarget> findAll();

    SavedJvmTarget save(SavedJvmTarget target);

    void deleteById(String id);

    void markConnected(String id, Instant connectedAt);
}
