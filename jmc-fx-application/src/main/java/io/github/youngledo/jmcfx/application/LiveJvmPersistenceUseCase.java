package io.github.youngledo.jmcfx.application;

import java.time.Instant;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.SavedJvmTarget;
import io.github.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;

public final class LiveJvmPersistenceUseCase {

    private final SavedJvmTargetRepository repository;

    public LiveJvmPersistenceUseCase(SavedJvmTargetRepository repository) {
        this.repository = repository;
    }

    public boolean available() {
        return repository != null;
    }

    public List<SavedJvmTarget> findAll() {
        return repository.findAll();
    }

    public SavedJvmTarget save(SavedJvmTarget target) {
        return repository.save(target);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }

    public void markConnected(String id, Instant connectedAt) {
        repository.markConnected(id, connectedAt);
    }
}
