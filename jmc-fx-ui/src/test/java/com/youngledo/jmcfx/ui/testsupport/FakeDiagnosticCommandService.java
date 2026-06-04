package com.youngledo.jmcfx.ui.testsupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandRequest;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.DiagnosticCommandService;
import com.youngledo.jmcfx.domain.service.JmcFxException;

public class FakeDiagnosticCommandService implements DiagnosticCommandService {

    private final Map<String, List<DiagnosticCommandInfo>> commands = new HashMap<>();
    private final Map<Key, DiagnosticCommandResult> results = new HashMap<>();
    private RuntimeException failure;
    private DiagnosticCommandRequest lastRequest;

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public void setCommands(String connectionId, List<DiagnosticCommandInfo> rows) {
        commands.put(Objects.requireNonNullElse(connectionId, ""), List.copyOf(rows));
    }

    public void setResult(String connectionId, String commandName, DiagnosticCommandResult result) {
        results.put(new Key(connectionId, commandName), result);
    }

    public DiagnosticCommandRequest lastRequest() {
        return lastRequest;
    }

    @Override
    public List<DiagnosticCommandInfo> commands(JvmConnection connection) {
        failIfConfigured();
        return List.copyOf(commands.getOrDefault(connectionId(connection), List.of()));
    }

    @Override
    public DiagnosticCommandResult execute(DiagnosticCommandRequest request) {
        failIfConfigured();
        lastRequest = request;
        DiagnosticCommandResult result = results.get(new Key(request.connection().id(), request.commandName()));
        if (result == null) {
            throw new JmcFxException("No fake Diagnostic Command result for " + request.commandName());
        }
        return result;
    }

    private void failIfConfigured() {
        if (failure != null) {
            throw failure;
        }
    }

    private static String connectionId(JvmConnection connection) {
        return connection == null ? "" : connection.id();
    }

    private record Key(String connectionId, String commandName) {
        private Key {
            connectionId = Objects.requireNonNullElse(connectionId, "");
            commandName = Objects.requireNonNullElse(commandName, "");
        }
    }
}
